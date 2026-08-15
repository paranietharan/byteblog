package com.paranietharan.byteblog.repository;

import java.util.UUID;

public interface PostTagProjection {
    UUID getPostId();
    String getTagName();
}
