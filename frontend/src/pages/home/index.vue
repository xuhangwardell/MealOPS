<template>
    <AppPage title="MealOps" subtitle="为一人食安排未来多餐，让库存、时间与购物计划保持一致。">
        <view class="status-card surface-card">
            <view class="status-heading">
                <text class="section-title">后端连接</text>
                <text :class="statusClass">{{ statusLabel }}</text>
            </view>
            <text class="status-detail">{{ statusDetail }}</text>
            <text v-if="lastCheckedAt" class="checked-at">上次检查：{{ formattedCheckedAt }}</text>
            <button class="check-button" :loading="backendStatus === 'checking'"
                    :disabled="backendStatus === 'checking'" @click="checkBackendHealth">
                {{ backendStatus === "checking" ? "检查中" : "重新检查" }}
            </button>
        </view>

        <view class="entry-section">
            <text class="section-title">开始整理你的用餐安排</text>
            <view class="entry-grid">
                <view v-for="entry in entries" :key="entry.title" class="entry-card surface-card">
                    <text class="entry-title">{{ entry.title }}</text>
                    <text class="entry-description">{{ entry.description }}</text>
                </view>
            </view>
        </view>
    </AppPage>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { storeToRefs } from "pinia";
import AppPage from "@/components/AppPage.vue";
import { useAppStore } from "@/stores/app";

const store = useAppStore();
const { backendStatus, lastCheckedAt, lastError } = storeToRefs(store);
const { checkBackendHealth } = store;

const entries = [
    { title: "库存", description: "记录手头食材与批次，为后续规划提供真实基础。" },
    { title: "菜谱", description: "维护结构化菜谱，让用量计算与复用更可靠。" },
    { title: "计划", description: "查看未来多餐安排，并衔接购物与实际完成。" }
];

const statusLabel = computed(() => ({
    unknown: "尚未检查",
    checking: "检查中",
    online: "在线",
    offline: "离线"
}[backendStatus.value]));
const statusClass = computed(() => ({
    unknown: "status-warning",
    checking: "status-warning",
    online: "status-success",
    offline: "status-error"
}[backendStatus.value]));
const statusDetail = computed(() => lastError.value ?? (backendStatus.value === "online"
    ? "业务服务运行正常，可以继续使用 MealOps。"
    : "MealOps 会通过健康检查确认本地业务服务是否可用。"));
const formattedCheckedAt = computed(() => lastCheckedAt.value === null
    ? ""
    : new Date(lastCheckedAt.value).toLocaleString());

onMounted(() => {
    if (backendStatus.value === "unknown") {
        void checkBackendHealth();
    }
});
</script>

<style scoped lang="scss">
.status-card,
.entry-section,
.entry-grid,
.entry-card {
    display: flex;
    flex-direction: column;
}

.status-card,
.entry-card {
    gap: $space-sm;
}

.status-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: $space-md;
}

.section-title,
.entry-title {
    font-size: $text-lg;
    font-weight: 650;
}

.status-detail,
.entry-description,
.checked-at {
    color: $color-text-secondary;
    font-size: $text-md;
    line-height: 1.6;
}

.checked-at {
    font-size: $text-sm;
}

.check-button {
    min-height: 84rpx;
    margin: $space-sm 0 0;
    border-radius: $radius-sm;
    background: $color-primary;
    color: #ffffff;
    font-size: $text-md;
}

.entry-section {
    gap: $space-md;
    margin-top: $space-xl;
}

.entry-grid {
    gap: $space-md;
}
</style>
