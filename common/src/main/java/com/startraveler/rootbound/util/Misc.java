package com.startraveler.rootbound.util;

import java.util.Optional;
import java.util.function.Function;

public class Misc {
    private Misc() {
    }

    public static <S, T> Function<S, Optional<T>> optionalWrapper(Function<S, T> supplier) {
        return supplier == null ? (source) -> Optional.empty() : (source) -> Optional.ofNullable(supplier.apply(source));
    }


}
