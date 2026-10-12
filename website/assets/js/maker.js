/* ============================================================
 *  在线捏脸工坊（彩蛋页 #shizuku）
 *  把安卓版的捏脸逻辑搬到网页：25 个部位、上千张贴图，Canvas 实时合成。
 *  绘制顺序与 App 内的 LayerOrder 保持一致（项圈压在头发之上、衣服之下）。
 * ============================================================ */
(function () {
  'use strict';

  var ASSET_BASE = 'maker/';          // 素材根目录（服务器上的 maker/）
  var CANVAS_SIZE = 600;

  // 与 App 的 render/LayerOrder.kt 完全一致的绘制顺序
  var DRAW_ORDER = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 18, 19, 17, 16, 15, 20, 21, 22, 23, 24];

  var state = {
    parts: [],
    comb: [],       // [{url, x, y, itmId, cId}]
    activePart: 0,
    ready: false,
    renderToken: 0
  };

  var imgCache = new Map();

  var el = {};
  var guideIndex = 0;

  /* ---------------------------------------------------------- 工具函数 */
  function assetUrl(url) {
    if (!url) return '';
    return ASSET_BASE + url.replace(/^\/?app\/image_maker\//, '');
  }

  function loadImage(url) {
    if (!url) return Promise.resolve(null);
    if (imgCache.has(url)) return imgCache.get(url);
    var p = new Promise(function (resolve) {
      var img = new Image();
      img.onload = function () { resolve(img); };
      img.onerror = function () { resolve(null); };
      img.src = url;
    });
    imgCache.set(url, p);
    return p;
  }

  function hexOfRgb(rgb) {
    return '#' + [rgb[0], rgb[1], rgb[2]].map(function (v) {
      return ('0' + v.toString(16)).slice(-2);
    }).join('');
  }

  function rgb(hex) {
    var v = hex.replace('#', '');
    return [parseInt(v.slice(0, 2), 16), parseInt(v.slice(2, 4), 16), parseInt(v.slice(4, 6), 16)];
  }

  function dist(a, b) {
    return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[1] - b[1]) * (a[1] - b[1]) + (a[2] - b[2]) * (a[2] - b[2]));
  }

  /* ---------------------------------------------------------- 渲染 */
  function render() {
    var canvas = el.canvas;
    var ctx = canvas.getContext('2d');
    var token = ++state.renderToken;
    ctx.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

    var jobs = DRAW_ORDER.map(function (idx) {
      var sel = state.comb[idx];
      if (!sel || !sel.url) return null;
      return loadImage(assetUrl(sel.url)).then(function (img) {
        return img ? { img: img, x: sel.x, y: sel.y } : null;
      });
    }).filter(Boolean);

    Promise.all(jobs).then(function (layers) {
      if (token !== state.renderToken) return;   // 有更新的渲染请求，丢弃这次
      layers.forEach(function (l) {
        if (l) ctx.drawImage(l.img, l.x, l.y, l.img.width, l.img.height);
      });
      return reapplyHairOverCloth(ctx, token);
    });
  }

  /**
   * 后发（後ろ髪）排在脸之前，所以它垂到胸前的那部分会被衣服盖住 —— 看起来像"头发穿模"。
   * 这里把「后发 ∩ 衣服范围」再补画一次压在衣服上，头发就自然垂在衣服外面了。
   */
  function reapplyHairOverCloth(ctx, token) {
    var back = state.comb[1];
    var cloth = state.comb[16];
    if (!back || !back.url || !cloth || !cloth.url) return Promise.resolve();

    return Promise.all([
      loadImage(assetUrl(back.url)),
      loadImage(assetUrl(cloth.url))
    ]).then(function (imgs) {
      if (token !== state.renderToken) return;
      var hair = imgs[0], clothImg = imgs[1];
      if (!hair || !clothImg) return;

      var tmp = document.createElement('canvas');
      tmp.width = CANVAS_SIZE;
      tmp.height = CANVAS_SIZE;
      var tctx = tmp.getContext('2d');
      // 1) 先画衣服的范围
      tctx.drawImage(clothImg, cloth.x, cloth.y, clothImg.width, clothImg.height);
      // 2) 只保留落在这块范围里的头发
      tctx.globalCompositeOperation = 'source-in';
      tctx.drawImage(hair, back.x, back.y, hair.width, hair.height);
      // 3) 盖到成品上
      ctx.drawImage(tmp, 0, 0);
    });
  }

  /* ---------------------------------------------------------- UI 构建 */
  function buildTabs() {
    el.tabs.innerHTML = '';
    state.parts.forEach(function (part, i) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'maker__tab' + (i === state.activePart ? ' is-active' : '');
      b.setAttribute('role', 'tab');
      b.title = part.pNm;
      var thumb = part.thumbUrl || (part.items[0] && part.items[0].thumbUrl) || '';
      b.innerHTML = '<img src="' + assetUrl(thumb) + '" alt="' + part.pNm + '"><span>' + part.pNm + '</span>';
      b.addEventListener('click', function () {
        state.activePart = i;
        buildTabs(); buildColors(); buildItems();
        el.status.textContent = '当前编辑：' + part.pNm + '（' + part.items.length + ' 款可选）';
      });
      el.tabs.appendChild(b);
    });
  }

  function buildColors() {
    var part = state.parts[state.activePart];
    var sel = state.comb[state.activePart] || {};
    el.colors.innerHTML = '';
    if (!part || !part.colors) return;
    var ids = Object.keys(part.colors);
    if (ids.length < 2) return;   // 单色部位不显示色板

    ids.forEach(function (cId) {
      var hex = part.colors[cId];
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'maker__color' + (String(sel.cId) === String(cId) ? ' is-active' : '');
      b.style.background = hex;
      b.title = hex;
      b.setAttribute('aria-label', '颜色 ' + hex);
      b.addEventListener('click', function () { pickColour(cId); });
      el.colors.appendChild(b);
    });
  }

  function buildItems() {
    var part = state.parts[state.activePart];
    var sel = state.comb[state.activePart] || {};
    el.items.innerHTML = '';
    if (!part) return;

    // 「无」选项
    var none = document.createElement('button');
    none.type = 'button';
    none.className = 'maker__item maker__item--none' + (!sel.url ? ' is-active' : '');
    none.title = '不要这个部位';
    none.innerHTML = '<i class="fa-solid fa-xmark"></i>';
    none.addEventListener('click', function () {
      state.comb[state.activePart] = { url: '', x: sel.x, y: sel.y, itmId: 0, cId: 0 };
      buildItems(); render();
      el.status.textContent = '已隐藏「' + part.pNm + '」';
    });
    el.items.appendChild(none);

    part.items.forEach(function (item, idx) {
      var b = document.createElement('button');
      b.type = 'button';
      b.className = 'maker__item' + (String(sel.itmId) === String(item.itmId) ? ' is-active' : '');
      b.title = '第 ' + (idx + 1) + ' 款';
      b.innerHTML = '<img src="' + assetUrl(item.thumbUrl) + '" alt="' + part.pNm + ' 第' + (idx + 1) + '款" loading="lazy">';
      b.addEventListener('click', function () { pickItem(item); });
      el.items.appendChild(b);
    });
  }

  /* ---------------------------------------------------------- 交互 */
  function pickItem(item) {
    var partIndex = state.activePart;
    var part = state.parts[partIndex];
    var prev = state.comb[partIndex] || {};
    var wantHex = prev.cId != null ? part.colors[String(prev.cId)] : null;
    var original = item.originals[0];

    // 优先沿用当前颜色（如果这款有这个颜色）
    if (wantHex) {
      var want = rgb(wantHex);
      var best = Infinity;
      item.originals.forEach(function (o) {
        var hex = part.colors[String(o.cId)];
        if (!hex) return;
        var d = dist(want, rgb(hex));
        if (d < best) { best = d; original = o; }
      });
    }

    state.comb[partIndex] = {
      url: original.url,
      x: prev.x != null ? prev.x : (part.x || 0),
      y: prev.y != null ? prev.y : (part.y || 0),
      itmId: item.itmId,
      cId: original.cId
    };
    buildItems(); buildColors(); render();
    el.status.textContent = '「' + part.pNm + '」换好了';
  }

  function pickColour(cId) {
    var partIndex = state.activePart;
    var part = state.parts[partIndex];
    var sel = state.comb[partIndex];
    if (!sel || !sel.url) {
      el.status.textContent = '先选一款贴图，再挑颜色';
      return;
    }
    // 在当前 item 里找该颜色对应的素材
    var item = part.items.filter(function (it) { return String(it.itmId) === String(sel.itmId); })[0];
    if (!item) return;
    var target = item.originals.filter(function (o) { return String(o.cId) === String(cId); })[0];
    if (!target) return;
    sel.url = target.url;
    sel.cId = target.cId;
    buildColors(); render();
    el.status.textContent = '换好颜色了';
  }

  function randomize() {
    state.parts.forEach(function (part, i) {
      if (!part.items.length) return;
      // 眼睛/脸/嘴这类必选部位一定要选，其余有 35% 概率留空
      var mustPick = ['顔', '右目', '左目', 'お口'].indexOf(part.pNm) >= 0;
      var prev = state.comb[i] || {};
      if (!mustPick && Math.random() < 0.35) {
        state.comb[i] = { url: '', x: prev.x, y: prev.y, itmId: 0, cId: 0 };
        return;
      }
      var item = part.items[Math.floor(Math.random() * part.items.length)];
      var original = item.originals[Math.floor(Math.random() * item.originals.length)];
      state.comb[i] = {
        url: original.url,
        x: prev.x != null ? prev.x : (part.x || 0),
        y: prev.y != null ? prev.y : (part.y || 0),
        itmId: item.itmId,
        cId: original.cId
      };
    });
    buildTabs(); buildColors(); buildItems(); render();
    el.status.textContent = '随机了一套，不满意可以再点一次～';
  }

  function resetAll() {
    state.comb = state.parts.map(function (part, i) {
      var d = state.defaults[i] || {};
      return { url: d.url || '', x: d.x != null ? d.x : (part.x || 0), y: d.y != null ? d.y : (part.y || 0), itmId: d.itmId || 0, cId: d.cId || 0 };
    });
    buildTabs(); buildColors(); buildItems(); render();
    el.status.textContent = '已经恢复成默认形象';
  }

  function savePng() {
    el.canvas.toBlob(function (blob) {
      if (!blob) return;
      var a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = 'shizuku-maker-' + Date.now() + '.png';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(function () { URL.revokeObjectURL(a.href); }, 4000);
      el.status.textContent = '图片已经下载啦';
    }, 'image/png');
  }

  /* ---------------------------------------------------------- 新手教程 */
  var STEPS = [
    {
      title: '欢迎来到捏脸工坊',
      text: '这里能拼出你自己的猫娘：<strong>25 个部位</strong>、一千多张贴图。雫带你走一遍，大概一分钟就懂了。'
    },
    {
      title: '第一步：选部位',
      text: '右边这一排小方块是<strong>部位</strong>——背景、后发、脸、眼睛、衣服……点一下就把"画布焦点"切到那个部位。'
    },
    {
      title: '第二步：挑贴图',
      text: '切好部位后，下面会出现这个部位的<strong>全部款式</strong>。左边第一个带叉号的是"不要这个部位"，其余每格都是一款。'
    },
    {
      title: '第三步：换颜色',
      text: '有颜色的部位会在贴图上方出现<strong>色板</strong>，点一下就换色。可换的颜色是固定的几档，跟安卓版一样。'
    },
    {
      title: '第四步：随时看效果',
      text: '左边是<strong>实时预览</strong>，每次点选都会立刻重画。觉得乱了就按「重置」，想看别人的搭配就按「随机一套」。'
    },
    {
      title: '最后：保存',
      text: '满意之后点「保存 PNG」，会下载一张 <strong>600×600</strong> 的透明背景图。想再来一只就再从第一步开始，玩得开心～'
    }
  ];

  function openGuide(i) {
    guideIndex = Math.max(0, Math.min(STEPS.length - 1, i));
    el.guideLayer.hidden = false;
    el.guideCounter.textContent = '第 ' + (guideIndex + 1) + ' / ' + STEPS.length + ' 步';
    el.guideTitle.textContent = STEPS[guideIndex].title;
    el.guideText.innerHTML = STEPS[guideIndex].text;
    el.guideNext.textContent = guideIndex === STEPS.length - 1 ? '开始捏吧' : '下一步';
    el.guideDots.innerHTML = STEPS.map(function (_, n) {
      return '<i class="' + (n === guideIndex ? 'is-on' : '') + '"></i>';
    }).join('');
  }

  function closeGuide() {
    el.guideLayer.hidden = true;
    state.guideClosed = true;      // 用户手动关过就不再自动弹
    try { localStorage.setItem('shizuku-maker-guide', '1'); } catch (e) { /* 无痕模式忽略 */ }
  }

  /* ---------------------------------------------------------- 显示 / 隐藏 */
  function setMakerVisible(show) {
    var section = document.getElementById('makerEgg');
    if (!section) return;
    section.hidden = !show;
    document.body.classList.toggle('maker-open', show);
    if (show) {
      window.scrollTo({ top: 0, behavior: 'auto' });
      if (state.ready) ensureLoaded();
    }
  }

  function syncFromHash() {
    var h = window.location.hash;
    setMakerVisible(h === '#shizuku');
    if (h === '#shizuku' && state.ready) ensureLoaded();
  }

  function ensureLoaded() {
    if (state.loadedOnce) return;
    state.loadedOnce = true;
    el.loading.hidden = false;
    Promise.all([
      fetch(ASSET_BASE + 'orderedLayers.json').then(function (r) { return r.json(); }),
      fetch(ASSET_BASE + 'defaultCombination.json').then(function (r) { return r.json(); })
    ]).then(function (res) {
      state.parts = res[0];
      state.defaults = res[1];
      state.comb = state.parts.map(function (part, i) {
        var d = state.defaults[i] || {};
        return {
          url: d.url || '',
          x: d.x != null ? d.x : (part.x || 0),
          y: d.y != null ? d.y : (part.y || 0),
          itmId: d.itmId || 0,
          cId: d.cId || 0
        };
      });
      state.ready = true;
      el.loading.hidden = true;
      buildTabs(); buildColors(); buildItems(); render();
      el.status.textContent = '素材已就绪：' + state.parts.length + ' 个部位，随便点着玩～';
      // 第一次进来自动弹教程（用户手动关过 / 已看过就不弹）
      var seen = false;
      try { seen = localStorage.getItem('shizuku-maker-guide') === '1'; } catch (e) { seen = false; }
      state.guideClosed = state.guideClosed || seen;
      if (!seen && !state.guideClosed) {
        setTimeout(function () {
          if (state.guideClosed) return;
          openGuide(0);
        }, 600);
      }
    }).catch(function (err) {
      el.loading.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i><span>素材没加载成功，刷新一次试试</span>';
      el.status.textContent = '加载失败：' + err.message;
    });
  }

  /* ---------------------------------------------------------- 启动 */
  function boot() {
    el.canvas = document.getElementById('makerCanvas');
    if (!el.canvas) return;     // 这页没有工坊，直接退出
    el.tabs = document.getElementById('makerTabs');
    el.colors = document.getElementById('makerColors');
    el.items = document.getElementById('makerItems');
    el.status = document.getElementById('makerStatus');
    el.loading = document.getElementById('makerLoading');
    el.guideLayer = document.getElementById('guideLayer');
    el.guideCounter = document.getElementById('guideCounter');
    el.guideTitle = document.getElementById('guideTitle');
    el.guideText = document.getElementById('guideText');
    el.guideDots = document.getElementById('guideDots');
    el.guideNext = document.getElementById('guideNext');

    document.getElementById('makerSave').addEventListener('click', savePng);
    document.getElementById('makerRandom').addEventListener('click', randomize);
    document.getElementById('makerReset').addEventListener('click', resetAll);
    document.getElementById('makerGuide').addEventListener('click', function () { openGuide(0); });
    document.getElementById('makerExit').addEventListener('click', function () {
      if (window.location.hash === '#shizuku') {
        history.replaceState(null, '', window.location.pathname + window.location.search);
      }
      setMakerVisible(false);
    });
    document.getElementById('guideSkip').addEventListener('click', closeGuide);
    document.getElementById('guideNext').addEventListener('click', function () {
      if (guideIndex === STEPS.length - 1) { closeGuide(); return; }
      openGuide(guideIndex + 1);
    });
    el.guideLayer.querySelector('.guide__mask').addEventListener('click', closeGuide);

    window.addEventListener('hashchange', syncFromHash);
    syncFromHash();
    // 如果直接以 #shizuku 打开，立即开始加载素材
    if (window.location.hash === '#shizuku') ensureLoaded();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }
})();
