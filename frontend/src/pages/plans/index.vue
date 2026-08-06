<template>
    <AppPage title="计划" subtitle="安排未来多餐，并衔接购物预览与实际完成。">
        <AsyncStateView empty empty-title="计划页面正在准备中"
                        empty-message="后续将在这里接入真实餐食计划；当前不会展示模拟数据。" />
    </AppPage>
</template>

<script setup lang="ts">
import AppPage from "@/components/AppPage.vue";
import AsyncStateView from "@/components/AsyncStateView.vue";
</script>
