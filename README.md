# SLG 联盟领土算法实验室

一个使用 Java 25、Maven 和 Swing 编写的可交互桌面演示，用网格地图展示 SLG 游戏中常见的联盟领土算法。

项目还包含 `com.slgx4.aoi.algorithm`：对 cloudwu/aoi 热点对 AOI 算法的 Java 语义复刻，
以及 `com.slgx4.aoi.AOIDemoApplication` 独立 Swing 入口。

## 已实现

- **BFS / Flood Fill**：点击地图后按距离逐层播放扩张动画；山脉和敌方领土不可穿越。
- **连通性校验**：从联盟主城执行 BFS，自动剥离无法回到主城的四连通领土。
- **并查集（Union-Find）**：实时统计当前联盟的领土连通块数量、最大连通块和总格数。
- **Marching Squares**：把二值占领格提取为明亮的联盟边界线，并随地图变化实时更新。
- **凸包圈地**：对联盟哨塔执行 Andrew 单调链凸包，再用射线法填充多边形内部格子。
- **Voronoi 据点分区**：按主城和哨塔的最近欧氏距离分配领土；不同联盟的等距格保持中立。
- **怪物范围搜索**：预置五类怪物，选择地图中心、类型和半径 R，以曼哈顿距离搜索并按距离排序、高亮结果。
- **动态 AOI 热点对**：复刻 watcher/marker、`R/2` 关键点、`R..2R` 热点对、版本失效与进入事件。

## 环境

- JDK：`D:\Program Files\Java\jdk-25.0.2`
- Maven：3.9+
- GUI：Java Swing（无额外运行时依赖）

在 PowerShell 中构建：

```powershell
$env:JAVA_HOME = 'D:\Program Files\Java\jdk-25.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean package
```

运行：

```powershell
java -jar target/territory-demo-1.0.0.jar
```

运行独立 AOI 图形演示：

```powershell
java -cp target/territory-demo-1.0.0.jar com.slgx4.aoi.AOIDemoApplication
```

运行测试：

```powershell
mvn test
```

## 交互方式

1. 在右侧选择当前联盟和 BFS 半径。
2. 选择地图点击工具，再点击地图：
   - `BFS 扩张`：播放逐层占领动画。
   - `占领单格`：手动修改格子归属。
   - `切断领土`：清空桥接格，然后点击 `连通校验` 观察失联领土剥离。
   - `编辑障碍`：添加或移除不可通行山脉。
   - `放置哨塔`：放置至少三座后点击 `凸包圈地`。
   - `设置主城`：移动连通性校验的根节点。
   - `怪物搜索`：点击搜索中心，在弹窗中选择具体怪物类型或全部类型，并设置半径 R。
3. 点击 `据点分区`，以双方主城与哨塔为站点重新生成 Voronoi 势力范围。
4. `重置示例` 会恢复预置地图。预置蓝方含一个失联连通块，便于立即观察算法效果。

地图中半透明色块表示归属，亮色轮廓来自 Marching Squares，虚线多边形是哨塔凸包，`◆` 是主城，三角形是哨塔。

怪物以带类型文字的圆形图标显示，右下角数字是等级。执行搜索后，命中怪物保持高亮，
非所选类型和搜索半径外的怪物会淡化。搜索范围满足
`|怪物.x - 中心.x| + |怪物.y - 中心.y| <= R`，命中怪物会发光，完整结果在右侧按距离从近到远排列。

## 项目结构

```text
src/main/java/com/slgx4/territory/
├── algorithm/   # 可独立复用、无 UI 依赖的算法
├── model/       # 网格、阵营、坐标和据点模型
└── ui/          # Swing 界面、绘制和交互动画
```

AOI 核心算法位于 `src/main/java/com/slgx4/aoi/algorithm/`，Swing 演示位于
`src/main/java/com/slgx4/aoi/`。实现/API 对照和演示说明见
[`docs/AOI_JAVA_PORT.md`](docs/AOI_JAVA_PORT.md)。

算法层不依赖 Swing，可直接抽取到游戏服务端；桌面层只负责输入、动画和渲染。
