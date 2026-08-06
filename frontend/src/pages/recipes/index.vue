<template>
    <AppPage title="菜谱" subtitle="结构化维护每道菜的食材、份量和烹饪步骤。">
        <AsyncStateView empty empty-title="菜谱页面正在准备中"
                        empty-message="后续将在这里接入真实菜谱；当前不会展示模拟数据。" />
    </AppPage>
</template>

<script setup lang="ts">
import AppPage from "@/components/AppPage.vue";
import AsyncStateView from "@/components/AsyncStateView.vue";
</script>
