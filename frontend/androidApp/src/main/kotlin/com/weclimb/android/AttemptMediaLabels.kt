package com.weclimb.android

import com.weclimb.media.AttemptMediaState

fun AttemptMediaState.label(): String = when (this) {
    AttemptMediaState.NONE -> "영상 없음"
    AttemptMediaState.TRIM_PENDING -> "나중에 자르기"
    AttemptMediaState.ORIGINAL_KEPT -> "원본 유지"
    AttemptMediaState.TRIM_PROCESSING -> "트리밍 중"
    AttemptMediaState.TRIMMED -> "트리밍 완료"
    AttemptMediaState.TRIM_FAILED -> "트리밍 재시도 필요"
}
