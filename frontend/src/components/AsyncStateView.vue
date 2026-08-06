<template>
    <view v-if="loading" class="state surface-card" aria-live="polite">
        <text class="state-title">正在加载</text>
        <text class="state-message">请稍候，正在获取最新信息。</text>
    </view>
    <view v-else-if="error" class="state surface-card" aria-live="polite">
        <text class="state-title status-error">暂时无法加载</text>
        <text class="state-message">{{ error }}</text>
        <button v-if="retryable" class="state-action" @click="$emit('retry')">重新尝试</button>
    </view>
    <view v-else-if="empty" class="state surface-card">
        <text class="state-title">{{ emptyTitle }}</text>
        <text class="state-message">{{ emptyMessage }}</text>
        <slot name="empty" />
    </view>
    <slot v-else />
</template>

<script setup lang="ts">
withDefaults(defineProps<{
    loading?: boolean;
    error?: string | null;
    empty?: boolean;
    emptyTitle?: string;
    emptyMessage?: string;
    retryable?: boolean;
}>(), {
    loading: false,
    error: null,
    empty: false,
    emptyTitle: "暂无内容",
    emptyMessage: "这里还没有可显示的信息。",
    retryable: false
});

defineEmits<{ retry: [] }>();
</script>

<style scoped lang="scss">
.state { display: flex; flex-direction: column; align-items: flex-start; gap: $space-sm; }
.state-title { font-size: $text-lg; font-weight: 650; }
.state-message { color: $color-text-secondary; font-size: $text-md; line-height: 1.6; }
.state-action { min-height: 80rpx; margin: $space-sm 0 0; padding: 0 $space-lg; border-radius: $radius-sm; background: $color-primary; color: #fff; font-size: $text-md; }
</style>
