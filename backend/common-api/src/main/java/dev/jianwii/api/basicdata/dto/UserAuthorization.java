package dev.jianwii.api.basicdata.dto;

import java.util.Set;

/** 每次請求取得的帳號狀態與有效功能權限，不存入 session。 */
public record UserAuthorization(boolean active, Set<String> permissions) {}
