package com.slgx4.territory.algorithm;

public final class UnionFind {
    private final int[] parent;
    private final int[] size;

    public UnionFind(int elementCount) {
        if (elementCount < 0) {
            throw new IllegalArgumentException("元素数量不能为负数");
        }
        parent = new int[elementCount];
        size = new int[elementCount];
        for (int i = 0; i < elementCount; i++) {
            parent[i] = i;
            size[i] = 1;
        }
    }

    public int find(int element) {
        if (parent[element] != element) {
            parent[element] = find(parent[element]);
        }
        return parent[element];
    }

    public boolean union(int first, int second) {
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (size[firstRoot] < size[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }
        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        return true;
    }

    public boolean connected(int first, int second) {
        return find(first) == find(second);
    }

    public int componentSize(int element) {
        return size[find(element)];
    }
}

