package me.wobble.wauctionhouse.gui;

import java.util.Collections;
import java.util.List;

public final class Pagination<T> {

    public List<T> page(List<T> source, int page, int pageSize) {
        if (source == null || source.isEmpty() || pageSize <= 0 || page <= 0) {
            return Collections.emptyList();
        }

        int from = (page - 1) * pageSize;
        if (from >= source.size()) {
            return Collections.emptyList();
        }

        int to = Math.min(from + pageSize, source.size());
        return source.subList(from, to);
    }

    public int maxPage(List<T> source, int pageSize) {
        if (source == null || source.isEmpty() || pageSize <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) source.size() / pageSize);
    }
}
