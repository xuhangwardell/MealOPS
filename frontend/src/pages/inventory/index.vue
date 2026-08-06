<template>
    <AppPage title="库存" subtitle="统一查看手头食材，为规划和购物计算提供依据。">
        <AsyncStateView empty empty-title="库存页面正在准备中"
                        empty-message="后续将在这里接入真实库存批次；当前不会展示模拟数据。" />
    </AppPage>
</template>

<script setup lang="ts">
import AppPage from "@/components/AppPage.vue";
import AsyncStateView from "@/components/AsyncStateView.vue";
</script>
