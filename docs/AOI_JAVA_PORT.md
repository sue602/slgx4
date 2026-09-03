# cloudwu/aoi Java 语义复刻

`com.slgx4.aoi.algorithm` 是对 cloudwu/aoi 提交
`54660b509d6f91bba24d0307c565a5f355508812` 的 Java 25 语义移植。
实现保留原算法的对象状态、热点对、严格距离边界和消息方向；Java 的 GC 与
`Map` 分别替代了 C 版本的可注入分配器和自定义 `uint32_t` 哈希表。

## API 对照

| C API / 数据 | Java API / 数据 |
| --- | --- |
| `struct aoi_space *aoi_new()` | `AoiSpace.create()` |
| `aoi_update(space, id, mode, pos)` | `space.update(id, mode, AoiPosition)` |
| `aoi_message(space, cb, ud)` | `space.message(AoiCallback)` |
| `aoi_release(space)` | `space.release()` / `space.close()` |
| `uint32_t id` | 范围受检的 `long`（`0..0xffffffffL`） |
| `float pos[3]` | `AoiPosition(float x, float y, float z)` |
| callback `(watcher, marker)` | `AoiCallback.onMessage(watcherId, markerId)` |

另外提供 Java 便捷接口 `space.message()`，直接返回不可变的 `List<AoiEvent>`；
`objects()`、`hotPairs()` 和 `stats()` 是只读观测接口，供测试、监控和演示绘制使用，
不改变 AOI 算法。

## 原语义

- 半径固定为 `R = 10`，使用三维欧氏距离平方计算。
- 模式字符串为 `w`（watcher）、`m`（marker）、`wm` 和 `d`（drop）；未知字符忽略。
- 只有首次进入 AOI 半径的消息，没有离开消息。
- 对象相对关键点移动距离严格小于 `R/2` 时不进入移动集合；恰好等于 `R/2` 会更新版本。
- 距离严格小于 `R` 发送消息；`R..2R` 保存为热点对；严格大于 `2R` 忽略或移除。
- 每个 tick 先刷新已有热点对，再按原顺序比较：静态 watcher × 移动 marker、
  移动 watcher × 静态 marker、移动 watcher × 移动 marker。
- 同一对象同时是 watcher 和 marker 时，消息仍有方向，且不会与自己配对。

`AoiSpaceTest.referenceTestCScenarioProducesTheSameFiveMessagesInOrder` 完整运行原仓库
`test.c` 的 100 tick 场景，断言得到相同的五条 `(watcher, marker)` 消息序列。

核心算法和公开数据类型全部位于 `com.slgx4.aoi.algorithm`；
`com.slgx4.aoi` 只保留 Swing 演示入口与界面代码。

## 图形演示

先构建：

```powershell
$env:JAVA_HOME = 'D:\Program Files\Java\jdk-25.0.2'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn clean package
```

再启动独立入口：

```powershell
java -cp target/territory-demo-1.0.0.jar com.slgx4.aoi.AOIDemoApplication
```

演示默认逐项复刻 `test.c` 的四个对象、速度、100 tick 世界回绕和第 50 tick
删除对象 3。界面会同时显示 watcher 的 `R` 圈、所选对象关键点的 `R/2` 圈、
`2R` 热点范围、当前热点对和刚产生的有向 AOI 消息。

可以播放、暂停、单步或重置；也可以拖动对象、切换 `w/m/wm`、删除对象，或在地图上
点选新增对象。手动操作会立即执行 `update + message`，方便观察临界距离行为。

## 参考

- 云风，《[服务器端 AOI 的实现](https://blog.codingnow.com/2012/03/dev_note_13.html)》
- `[cloudwu/aoi](https://github.com/cloudwu/aoi)`
- 复刻基准提交：`54660b509d6f91bba24d0307c565a5f355508812`
