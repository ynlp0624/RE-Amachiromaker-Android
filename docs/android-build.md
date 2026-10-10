# RE:Amachiromaker — Android 原生版

把 `RE-Amachiromaker-deploy`（React 静态站）移植成的**安卓原生应用**。

**不是 WebView 套壳**：没有 `WebView`、没有 `loadUrl`、没有内嵌浏览器、没有 Cordova/Capacitor/Electron 之类的壳。
界面是 Kotlin + Jetpack Compose 从零重写的原生 UI，贴图直接从 APK assets 解码到 `Bitmap`，
用 `android.graphics.Canvas` 分层合成，导出的 PSD 由纯 Kotlin 手写 PSD 容器生成。
完全离线运行，不联网、不依赖 CDN。

---

## 一、功能对照（网页版 → 原生版）

| 网页版 | 原生版 |
|---|---|
| 25 个部位 Tab（缩略图 + 名称） | `PartTabs`：横向滚动原生 Tab |
| 贴图网格选择 | `LazyVerticalGrid` 网格 |
| 「无」空槽（`emptyset.svg`） | `EmptyTile`：原生虚线框 + 叉 |
| 颜色球选择 | `ColourRow`：圆形色块（读取 `colors` 表） |
| 实时合成预览（merge-images, 600×600） | `Compositor`：Canvas 分层绘制到 600×600 Bitmap |
| Save PNG | 存进系统图库 `Pictures/Amachiromaker/`（MediaStore） |
| Save PSD（ag-psd） | `PsdWriter`：手写分层 PSD（每层 RGBA + 图层名 + 位置 + 合成图） |
| Reset | 重新载入 `defaultCombination.json` |
| Load / Download Preset | 系统文件选择器读写 `.json` 预设（SAF） |
| localStorage 持久化 | SharedPreferences（键名沿用 `layerComb`） |
| 深色/浅色主题 | Material3 深浅配色 + 顶栏手动切换 |
| 使用须知 / 关于本站 | 原生对话框，版权与作者信息完整保留 |

数据格式与素材与原站完全一致：`orderedLayers.json`、`defaultCombination.json`、
`image_maker/168503/...`（1111 张 PNG 全部打包进 APK）。

---

## 二、构建

需要 JDK 17 + Android SDK（compileSdk 34）。本机现成环境：

```powershell
$env:JAVA_HOME  = 'D:\Android\jdk-17'
$env:ANDROID_HOME = 'D:\Android\Sdk'
D:\Android\gradle-8.7\bin\gradle.bat :app:assembleDebug --console=plain
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

安装到模拟器 / 真机：

```powershell
D:\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
D:\Android\Sdk\platform-tools\adb.exe shell am start -n cc.nekoha.amachiromaker/.MainActivity
```

---

## 三、工程结构

```
AmachiromakerApp/
├─ app/src/main/
│  ├─ assets/
│  │  ├─ data/orderedLayers.json      25 个部位定义
│  │  ├─ data/defaultCombination.json 默认搭配
│  │  └─ image_maker/168503/...       1111 张 PNG（约 32 MB）
│  ├─ java/cc/nekoha/amachiromaker/
│  │  ├─ MainActivity.kt              入口 + 主题/须知状态
│  │  ├─ model/Models.kt              数据模型（kotlinx.serialization）
│  │  ├─ data/AssetStore.kt           assets 读取 + Bitmap LRU 缓存
│  │  ├─ data/MakerState.kt           组合状态、持久化、预览调度
│  │  ├─ render/Compositor.kt         600×600 分层合成
│  │  ├─ export/PsdWriter.kt          纯 Kotlin 分层 PSD 写出
│  │  ├─ export/Exports.kt            MediaStore / SAF 文件读写
│  │  └─ ui/                          Theme / MakerScreen / PartPicker / Common
│  └─ res/                            图标、主题、字符串
└─ keystore/local.jks                 本地签名（口令 amachiromaker）
```

---

## 四、版权

- 贴图版权归 **甘城なつき**（[amashiro.com](https://amashiro.com/)）所有，请勿商用或二次售卖。
- 二改自 [NekoQiye/RE-Amachiromaker](https://github.com/NekoQiye/RE-Amachiromaker)（原作者 Charlie Chiangm）。
- 二改请保留上述标注。
