package com.rongzhiqiao.common.api;

import java.util.List;

public record PageResponse<T>(
        long total,
        int page,
        int pageSize,
        List<T> list
) {
}
