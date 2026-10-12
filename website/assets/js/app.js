/* RE:Amachiromaker 站点脚本：主题切换 / 移动菜单 / 图片灯箱 / 滚动动画 */
(function () {
  'use strict';

  /* ------------------------------------------------------------ 主题 */
  var root = document.documentElement;
  var STORAGE_KEY = 'amachiromaker-theme';
  var themeBtn = document.getElementById('themeToggle');
  var themeIcon = themeBtn ? themeBtn.querySelector('i') : null;

  function systemDark() {
    return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  function applyTheme(theme) {
    root.setAttribute('data-theme', theme);
    if (themeIcon) {
      themeIcon.className = theme === 'dark' ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
    }
    var meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'dark' ? '#091420' : '#3e93e0');
  }

  var saved = null;
  try { saved = localStorage.getItem(STORAGE_KEY); } catch (e) { saved = null; }
  applyTheme(saved || (systemDark() ? 'dark' : 'light'));

  if (themeBtn) {
    themeBtn.addEventListener('click', function () {
      var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      applyTheme(next);
      try { localStorage.setItem(STORAGE_KEY, next); } catch (e) { /* 忽略隐私模式下的写入失败 */ }
    });
  }

  if (window.matchMedia) {
    var mq = window.matchMedia('(prefers-color-scheme: dark)');
    var onSystemChange = function (e) {
      var stored = null;
      try { stored = localStorage.getItem(STORAGE_KEY); } catch (err) { stored = null; }
      if (!stored) applyTheme(e.matches ? 'dark' : 'light');
    };
    if (mq.addEventListener) mq.addEventListener('change', onSystemChange);
    else if (mq.addListener) mq.addListener(onSystemChange);
  }

  /* --------------------------------------------------------- 移动菜单 */
  var menuBtn = document.getElementById('menuToggle');
  var navLinks = document.getElementById('navLinks');
  if (menuBtn && navLinks) {
    menuBtn.addEventListener('click', function () {
      var open = navLinks.classList.toggle('open');
      var icon = menuBtn.querySelector('i');
      if (icon) icon.className = open ? 'fa-solid fa-xmark' : 'fa-solid fa-bars';
    });
    navLinks.addEventListener('click', function (e) {
      if (e.target.tagName === 'A') {
        navLinks.classList.remove('open');
        var icon = menuBtn.querySelector('i');
        if (icon) icon.className = 'fa-solid fa-bars';
      }
    });
  }

  /* ----------------------------------------------------- 导航栏阴影 */
  var nav = document.getElementById('nav');
  var toTop = document.getElementById('toTop');
  function onScroll() {
    var y = window.scrollY || document.documentElement.scrollTop;
    if (nav) nav.classList.toggle('is-stuck', y > 8);
    if (toTop) toTop.classList.toggle('show', y > 600);
  }
  window.addEventListener('scroll', onScroll, { passive: true });
  onScroll();

  if (toTop) {
    toTop.addEventListener('click', function () {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    });
  }

  /* --------------------------------------------------------- 滚动出现 */
  var revealTargets = document.querySelectorAll('.card, .shot, .panel, .mini, .table-wrap, .download__main, .file-card, .steps, .section__head');
  if ('IntersectionObserver' in window) {
    revealTargets.forEach(function (el, i) {
      el.classList.add('reveal');
      el.style.transitionDelay = (Math.min(i % 6, 5) * 45) + 'ms';
    });
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-in');
          io.unobserve(entry.target);
        }
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.06 });
    revealTargets.forEach(function (el) { io.observe(el); });

    // 兜底：无论滚动方式如何，几秒后一律显示，避免出现空白区块
    window.setTimeout(function () {
      document.querySelectorAll('.reveal:not(.is-in)').forEach(function (el) {
        el.classList.add('is-in');
      });
    }, 2600);
  } else {
    revealTargets.forEach(function (el) { el.classList.add('is-in'); });
  }

  /* ----------------------------------------------------------- 灯箱 */
  var figures = Array.prototype.slice.call(document.querySelectorAll('#galleryGrid .shot'));
  var lightbox = document.getElementById('lightbox');
  var lightboxImg = document.getElementById('lightboxImg');
  var lightboxCap = document.getElementById('lightboxCap');
  var closeBtn = document.getElementById('lightboxClose');
  var prevBtn = document.getElementById('lightboxPrev');
  var nextBtn = document.getElementById('lightboxNext');
  var current = 0;

  function showFigure(index) {
    if (!figures.length) return;
    current = (index + figures.length) % figures.length;
    var figure = figures[current];
    var img = figure.querySelector('img');
    lightboxImg.src = img.getAttribute('src');
    lightboxImg.alt = img.getAttribute('alt') || '';
    lightboxCap.textContent = figure.getAttribute('data-title') || '';
    lightbox.hidden = false;
    document.body.style.overflow = 'hidden';
  }

  function closeLightbox() {
    lightbox.hidden = true;
    lightboxImg.src = '';
    document.body.style.overflow = '';
  }

  figures.forEach(function (figure, i) {
    figure.addEventListener('click', function () { showFigure(i); });
  });

  if (closeBtn) closeBtn.addEventListener('click', closeLightbox);
  if (prevBtn) prevBtn.addEventListener('click', function (e) { e.stopPropagation(); showFigure(current - 1); });
  if (nextBtn) nextBtn.addEventListener('click', function (e) { e.stopPropagation(); showFigure(current + 1); });
  if (lightbox) {
    lightbox.addEventListener('click', function (e) {
      if (e.target === lightbox) closeLightbox();
    });
  }
  document.addEventListener('keydown', function (e) {
    if (!lightbox || lightbox.hidden) return;
    if (e.key === 'Escape') closeLightbox();
    if (e.key === 'ArrowLeft') showFigure(current - 1);
    if (e.key === 'ArrowRight') showFigure(current + 1);
  });

  /* ------------------------------------------------------- 复制哈希 */
  document.querySelectorAll('.copy-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var text = btn.getAttribute('data-copy') || '';
      var label = btn.querySelector('span');
      var icon = btn.querySelector('i');
      var done = function () {
        btn.classList.add('done');
        if (label) label.textContent = '已复制';
        if (icon) icon.className = 'fa-solid fa-check';
        setTimeout(function () {
          btn.classList.remove('done');
          if (label) label.textContent = '复制';
          if (icon) icon.className = 'fa-solid fa-copy';
        }, 1800);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(done, function () { /* 忽略 */ });
      } else {
        var ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        try { document.execCommand('copy'); done(); } catch (err) { /* 忽略 */ }
        document.body.removeChild(ta);
      }
    });
  });
  /* -------------------------------------------------- 多源智能下载 */
  // 所有节点都部署了同一份站点；前端并行测速后从最快的源下载。
  // 想加节点：在下面数组里加一项即可（apk 字段可覆盖默认的安装包路径）。
  var MIRRORS = [
    {
      id: 'gitcode',
      label: 'GitCode 源 · 国内直连',
      // GitCode 的 CDN 不带 CORS，前端读不到字节数；样本大小已知，改用「no-cors 下载计时」估算速度
      noCors: true,
      probeBytes: 524288,
      probe: 'https://api.gitcode.com/api/v5/repos/gcw_YiSvwnI0/RE-Amachiromaker-Android/releases/v1.0.2/attach_files/speedtest.bin/download',
      apk: 'https://gitcode.com/gcw_YiSvwnI0/RE-Amachiromaker-Android/releases/download/v1.0.2/RE-Amachiromaker-v1.0.2-android.apk'
    },
    { id: 'srv3', label: '主节点 · 独立线路', base: 'https://gallery.shizuku.plus/' },
    { id: 'srv2', label: '镜像节点 · 备用线路', base: 'https://gallery-s2.shizuku.plus/' },
    {
      id: 'github',
      label: 'GitHub 源 · 官方直连',
      // 测速走 raw（带 CORS），下载走 Release 直链；两者都是 GitHub 官方域名
      base: 'https://raw.githubusercontent.com/ynlp0624/RE-Amachiromaker-Android/main/',
      apk: 'https://github.com/ynlp0624/RE-Amachiromaker-Android/releases/download/v1.0.2/RE-Amachiromaker-v1.0.2-android.apk'
    },
    {
      id: 'github-proxy',
      label: 'GitHub 源 · 加速代理（备选）',
      // 直连被阻断时才用得上，走 gh-proxy 转发
      base: 'https://gh-proxy.com/https://raw.githubusercontent.com/ynlp0624/RE-Amachiromaker-Android/main/',
      apk: 'https://gh-proxy.com/https://github.com/ynlp0624/RE-Amachiromaker-Android/releases/download/v1.0.2/RE-Amachiromaker-v1.0.2-android.apk'
    }
  ];
  var APK_PATH = 'assets/download/RE-Amachiromaker-v1.0.2-android.apk';
  var PROBE_URL = 'speedtest.bin';
  var PROBE_TIMEOUT = 7000;      // 单个节点最多等 7 秒
  var PROBE_DEADLINE = 5500;     // 到点即汇总，不再等慢节点
  var BEST_KEY = 'amachiromaker-best-mirror';

  function apkUrlOf(mirror) {
    return mirror.apk || (mirror.base + APK_PATH);
  }

  var smartBtn = document.getElementById('smartDownload');
  var toggleBtn = document.getElementById('mirrorToggle');
  var listEl = document.getElementById('mirrorList');
  var statusEl = document.getElementById('mirrorStatus');

  function readBest() {
    try { return localStorage.getItem(BEST_KEY) || ''; } catch (e) { return ''; }
  }
  function writeBest(id) {
    try { localStorage.setItem(BEST_KEY, id); } catch (e) { /* 忽略 */ }
  }
  function formatSpeed(bytesPerSecond) {
    if (!bytesPerSecond || !isFinite(bytesPerSecond)) return '—';
    if (bytesPerSecond >= 1024 * 1024) return (bytesPerSecond / 1024 / 1024).toFixed(1) + ' MB/s';
    return Math.round(bytesPerSecond / 1024) + ' KB/s';
  }

  // 拉取样本数据并统计真实耗时（含首个字节延迟）；超时但已有足够数据时按已完成部分估算
  function probe(mirror, onProgress) {
    var url = (mirror.probe || (mirror.base + PROBE_URL)) + '?t=' + Date.now();
    var controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
    var received = 0;
    var started = (window.performance && performance.now) ? performance.now() : Date.now();
    var now = function () {
      return (window.performance && performance.now) ? performance.now() : Date.now();
    };
    var timer = window.setTimeout(function () { if (controller) controller.abort(); }, PROBE_TIMEOUT);
    var count = function (bytes) {
      received = bytes;
      if (onProgress) onProgress(bytes);
    };

    // 没有 CORS 的节点（如 GitCode 的 CDN）：样本大小已知，用整体耗时反推速度
    if (mirror.noCors) {
      var expected = mirror.probeBytes || 524288;
      return fetch(url, { mode: 'no-cors', cache: 'no-store' })
        .then(function () {
          window.clearTimeout(timer);
          var seconds = Math.max((now() - started) / 1000, 0.001);
          count(expected);
          return { ok: true, bytes: expected, seconds: seconds, speed: expected / seconds, approximate: true };
        })
        .catch(function () {
          window.clearTimeout(timer);
          return { ok: false, reason: '无法连接', speed: 0 };
        });
    }

    return fetch(url, { cache: 'no-store', signal: controller ? controller.signal : undefined })
      .then(function (res) {
        if (!res.ok) throw new Error('HTTP ' + res.status);
        if (!res.body || !res.body.getReader) {
          return res.arrayBuffer().then(function (buf) { count(buf.length); return buf.length; });
        }
        var reader = res.body.getReader();
        return (function pump() {
          return reader.read().then(function (chunk) {
            if (chunk.done) return received;
            count(received + chunk.value.length);
            return pump();
          });
        })();
      })
      .then(function (bytes) {
        window.clearTimeout(timer);
        var seconds = Math.max((now() - started) / 1000, 0.001);
        return { ok: true, bytes: bytes, seconds: seconds, speed: bytes / seconds };
      })
      .catch(function (err) {
        window.clearTimeout(timer);
        var seconds = Math.max((now() - started) / 1000, 0.001);
        var aborted = err && err.name === 'AbortError';
        // 超时但已经拉到 64 KiB 以上：用已收到的数据估算，不直接判死
        if (aborted && received > 65536) {
          return { ok: true, partial: true, bytes: received, seconds: seconds, speed: received / seconds };
        }
        return { ok: false, reason: aborted ? '超时' : '无法连接', speed: 0 };
      });
  }

  function renderNodes(state) {
    listEl.innerHTML = '';
    state.forEach(function (entry) {
      var li = document.createElement('li');
      li.className = 'node' + (entry.status === 'probing' ? ' is-probing' : '') +
        (entry.status === 'dead' ? ' is-dead' : '') +
        (entry.fastest ? ' is-fastest' : '');

      var name = document.createElement('span');
      name.className = 'node__name';
      var icon = document.createElement('i');
      icon.className = entry.status === 'dead' ? 'fa-solid fa-triangle-exclamation'
        : entry.status === 'probing' ? 'fa-solid fa-spinner fa-spin'
        : 'fa-solid fa-circle-check';
      name.appendChild(icon);
      name.appendChild(document.createTextNode(entry.mirror.label));
      if (entry.fastest) {
        var badge = document.createElement('span');
        badge.className = 'node__badge';
        badge.textContent = '最快';
        name.appendChild(badge);
      }
      if (entry.remembered && !entry.fastest) {
        var badge2 = document.createElement('span');
        badge2.className = 'node__badge node__badge--muted';
        badge2.textContent = '上次最快';
        name.appendChild(badge2);
      }

      var speed = document.createElement('span');
      speed.className = 'node__speed';
      speed.textContent = entry.status === 'probing' ? '测速中…'
        : entry.status === 'dead' ? entry.reason
        : (entry.partial || entry.approximate ? '约 ' : '') + formatSpeed(entry.speed);

      var bar = document.createElement('span');
      bar.className = 'node__bar';
      var fill = document.createElement('span');
      fill.className = 'node__fill';
      var pct = entry.status === 'probing' ? Math.min(90, (entry.progress || 0) / 524288 * 100)
        : entry.status === 'dead' ? 0
        : Math.min(100, entry.speed / 1024 / 1024 * 100);
      fill.style.width = entry.status === 'dead' ? '0%' : Math.max(4, pct) + '%';
      bar.appendChild(fill);

      li.appendChild(name);
      li.appendChild(speed);
      li.appendChild(bar);
      listEl.appendChild(li);
    });
  }

  function startDownload(mirror, auto) {
    var url = apkUrlOf(mirror);
    var a = document.createElement('a');
    a.href = url;
    a.setAttribute('download', 'RE-Amachiromaker-v1.0.2-android.apk');
    a.rel = 'noopener';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    if (statusEl) {
      statusEl.hidden = false;
      statusEl.className = 'mirror__status is-done';
      statusEl.innerHTML = '<i class="fa-solid fa-circle-check"></i><span>' +
        (auto ? '已从最快节点「' + mirror.label + '」开始下载' : '已从「' + mirror.label + '」开始下载') +
        '，请留意浏览器的下载提示。</span>';
    }
  }

  function runProbe(autoDownload) {
    if (!listEl || !statusEl) return;
    var remembered = readBest();
    var state = MIRRORS.map(function (mirror) {
      return { mirror: mirror, status: 'probing', progress: 0, speed: 0, remembered: mirror.id === remembered };
    });

    listEl.hidden = false;
    statusEl.hidden = false;
    statusEl.className = 'mirror__status';
    statusEl.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i><span>正在并行测速，最多等 ' +
      Math.round(PROBE_DEADLINE / 1000) + ' 秒…</span>';
    renderNodes(state);

    var settled = false;

    function settle() {
      if (settled) return;
      settled = true;

      // 到点还没回话的节点：有数据就按时长估算，没数据算它不可用
      state.forEach(function (entry) {
        if (entry.status !== 'probing') return;
        if ((entry.progress || 0) > 65536) {
          entry.status = 'done';
          entry.partial = true;
          entry.speed = entry.progress / (PROBE_DEADLINE / 1000);
        } else {
          entry.status = 'dead';
          entry.reason = '未响应';
        }
      });
      renderNodes(state);

      var alive = state.filter(function (e) { return e.status === 'done' && e.speed > 0; });
      if (!alive.length) {
        statusEl.className = 'mirror__status';
        statusEl.innerHTML = '<i class="fa-solid fa-triangle-exclamation"></i><span>所有节点都没测通，直接用当前页面所在节点下载。</span>';
        startDownload({ id: 'self', label: '当前节点', base: location.origin + '/', apk: APK_PATH }, autoDownload);
        return;
      }
      alive.sort(function (a, b) { return b.speed - a.speed; });
      alive[0].fastest = true;
      writeBest(alive[0].mirror.id);
      renderNodes(state);
      if (autoDownload) {
        startDownload(alive[0].mirror, true);
      } else {
        statusEl.className = 'mirror__status is-done';
        statusEl.innerHTML = '<i class="fa-solid fa-circle-check"></i><span>最快是「' + alive[0].mirror.label +
          '」（' + formatSpeed(alive[0].speed) + '），点上方按钮即可从它下载。</span>';
      }
    }

    var pending = state.length;
    state.forEach(function (entry) {
      probe(entry.mirror, function (bytes) {
        if (settled) return;
        entry.progress = bytes;
        renderNodes(state);
      }).then(function (result) {
        if (settled) return;
        entry.status = result.ok ? 'done' : 'dead';
        entry.speed = result.speed;
        entry.partial = result.partial;
        entry.approximate = result.approximate;
        entry.reason = result.reason || '不可用';
        pending -= 1;
        renderNodes(state);
        if (pending === 0) settle();
      });
    });

    window.setTimeout(settle, PROBE_DEADLINE);
  }

  if (smartBtn) {
    smartBtn.addEventListener('click', function () { runProbe(true); });
  }

  // 页面加载时：若本机记过更快的节点，把普通下载按钮直接指过去（零额外流量）
  (function retargetDownloadButtons() {
    var best = readBest();
    if (!best) return;
    var mirror = null;
    for (var i = 0; i < MIRRORS.length; i++) {
      if (MIRRORS[i].id === best) mirror = MIRRORS[i];
    }
    if (!mirror) return;
    var links = document.querySelectorAll('a[href$="RE-Amachiromaker-v1.0.2-android.apk"]');
    Array.prototype.forEach.call(links, function (a) {
      var target = apkUrlOf(mirror);
      if (a.href !== target) a.href = target;
    });
  })();
  if (toggleBtn) {
    toggleBtn.addEventListener('click', function () {
      var remembered = readBest();
      var state = MIRRORS.map(function (mirror) {
        return { mirror: mirror, status: 'idle', speed: 0, remembered: mirror.id === remembered };
      });
      if (listEl) listEl.hidden = false;
      var showing = toggleBtn.getAttribute('aria-expanded') === 'true';
      if (showing) {
        listEl.hidden = true;
        toggleBtn.setAttribute('aria-expanded', 'false');
        return;
      }
      toggleBtn.setAttribute('aria-expanded', 'true');
      renderNodes(state);
      if (statusEl) {
        statusEl.hidden = false;
        statusEl.className = 'mirror__status';
        statusEl.innerHTML = '<i class="fa-solid fa-circle-info"></i><span>点「测速并下载」会真实测一遍这几个节点；「上次最快」是这台设备记录的最优节点。</span>';
      }
      // 每个节点给一个手动下载入口
      Array.prototype.forEach.call(listEl.querySelectorAll('.node'), function (node, i) {
        node.style.cursor = 'pointer';
        node.title = '点击从该节点下载';
        node.addEventListener('click', function () { startDownload(MIRRORS[i], false); });
      });
    });
  }

  /* --------------------------------------------------------- 彩蛋路由 */
  // 地址栏加 #bugs 进入隐藏的「踩坑实录」页；页脚的虫子图标也能进
  var eggSection = document.getElementById('easterEgg');
  var eggBack = document.getElementById('eggBack');
  var eggCount = document.getElementById('eggCount');

  function showEgg(show) {
    if (!eggSection) return;
    eggSection.hidden = !show;
    document.body.classList.toggle('egg-open', show);
    if (show) {
      if (eggCount) {
        var total = eggSection.querySelectorAll('.egg-card').length;
        eggCount.textContent = '共记录 ' + total + ' 条';
      }
      window.scrollTo({ top: 0, behavior: 'auto' });
    }
  }

  function syncEggFromHash() {
    showEgg(window.location.hash === '#bugs');
  }
  window.addEventListener('hashchange', syncEggFromHash);
  syncEggFromHash();

  if (eggBack) {
    eggBack.addEventListener('click', function () {
      if (window.location.hash === '#bugs') {
        history.replaceState(null, '', window.location.pathname + window.location.search);
      }
      showEgg(false);
    });
  }

  var eggHint = document.getElementById('eggHint');
  if (eggHint) {
    eggHint.addEventListener('click', function () { window.location.hash = '#bugs'; });
  }
})();
