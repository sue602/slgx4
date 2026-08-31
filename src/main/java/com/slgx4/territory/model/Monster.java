package com.slgx4.territory.model;

import java.util.Objects;

public record Monster(String id, String name, MonsterType type, int level, GridPoint position) {
    public Monster {
        Objects.requireNonNull(id, "怪物编号不能为空");
        Objects.requireNonNull(name, "怪物名称不能为空");
        Objects.requireNonNull(type, "怪物类型不能为空");
        Objects.requireNonNull(position, "怪物坐标不能为空");
        if (id.isBlank() || name.isBlank()) {
            throw new IllegalArgumentException("怪物编号和名称不能为空白");
        }
        if (level <= 0) {
            throw new IllegalArgumentException("怪物等级必须大于 0");
        }
    }
}

