<template>
    <view class="app-page">
        <view class="page-heading">
            <text class="page-title">{{ title }}</text>
            <text v-if="subtitle" class="page-subtitle">{{ subtitle }}</text>
        </view>
        <slot />
    </view>
</template>

<script setup lang="ts">
defineProps<{
    title: string;
    subtitle?: string;
}>();
</script>

<style scoped lang="scss">
.app-page {
    width: 100%;
    max-width: 920rpx;
    min-height: 100vh;
    margin: 0 auto;
    padding: $space-lg $space-md calc($space-xl + env(safe-area-inset-bottom));
}

.page-heading {
    display: flex;
    flex-direction: column;
    gap: $space-sm;
    margin-bottom: $space-lg;
}

.page-title {
    font-size: $text-xl;
    font-weight: 700;
    line-height: 1.25;
}

.page-subtitle {
    color: $color-text-secondary;
    font-size: $text-md;
    line-height: 1.6;
}
</style>
