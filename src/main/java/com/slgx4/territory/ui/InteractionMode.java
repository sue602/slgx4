package com.slgx4.territory.ui;

public enum InteractionMode {
    BFS_EXPAND("BFS 扩张", "点击起点，按距离逐层占领"),
    OCCUPY("占领单格", "直接将格子设为当前联盟"),
    CUT("切断领土", "清空格子，再执行连通校验"),
    OBSTACLE("编辑障碍", "切换不可通行的山脉格"),
    OUTPOST("放置哨塔", "至少三个哨塔可执行凸包圈地"),
    CORE("设置主城", "设置连通性校验的根节点"),
    MONSTER_SEARCH("怪物搜索", "点击中心点，选择类型与半径 R");

    private final String displayName;
    private final String hint;

    InteractionMode(String displayName, String hint) {
        this.displayName = displayName;
        this.hint = hint;
    }

    public String displayName() {
        return displayName;
    }

    public String hint() {
        return hint;
    }
}
