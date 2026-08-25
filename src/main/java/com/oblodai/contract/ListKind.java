package com.oblodai.contract;

/** How a list route answers. */
public enum ListKind {
    /** {@code {items, paginate}} — walkable with limit/offset. */
    PAGED,
    /** {@code {items}} — capped by catalog size, not paginated. */
    PLAIN
}
