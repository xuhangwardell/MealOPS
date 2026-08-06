<template>
    <AppPage title="计划偏好" subtitle="保存未来规划使用的偏好设置">
        <view class="surface-card section">
            <text class="section-title">规划偏好</text>
            <input v-model="form.defaultServings" class="field" type="number" placeholder="默认份数">
            <view class="switch-row">
                <text>不限烹饪时间</text>
                <switch :checked="form.unlimitedCookingMinutes" @change="toggleUnlimited" />
            </view>
            <input v-if="!form.unlimitedCookingMinutes" v-model="form.maxCookingMinutes" class="field" type="number" placeholder="最长烹饪分钟数">
            <text class="subheading">排除食材</text>
            <checkbox-group @change="changeExcluded">
                <label v-for="item in ingredientStore.items" :key="item.id" class="check-row">
                    <checkbox :value="String(item.id)" :checked="form.excludedIngredientIds.includes(item.id)" />{{ item.name }}
                </label>
            </checkbox-group>
            <text v-if="ingredientStore.items.length === 0" class="row-detail">暂无可排除的食材</text>
            <button class="primary-button" :disabled="saving" @click="save">{{ saving ? "保存中…" : "保存偏好" }}</button>
            <text v-if="feedback" class="feedback">{{ feedback }}</text>
        </view>
        <view class="surface-card section muted-card">
            <text class="section-title">Meal planning</text>
            <text class="row-detail">多餐自动规划将在后续接入。</text>
        </view>
    </AppPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import AppPage from "@/components/AppPage.vue";
import { getPlanningPreferences, replacePlanningPreferences, type PlanningPreferences } from "@/api/planning-preferences";
import { useIngredientStore } from "@/stores/ingredients";
import { fromPlanningPreferences, toPlanningPreferencesRequest, type PlanningPreferencesFormState } from "@/forms/planning-preferences-form";

const ingredientStore = useIngredientStore();
const saving = ref(false);
const feedback = ref("");
const form = reactive<PlanningPreferencesFormState>({ defaultServings: "1", unlimitedCookingMinutes: true, maxCookingMinutes: "", excludedIngredientIds: [] });

function readable(value: unknown): string { return value instanceof Error ? value.message : "操作失败，请稍后重试"; }
function apply(value: PlanningPreferences): void { Object.assign(form, fromPlanningPreferences(value)); }
function toggleUnlimited(event: Event): void {
    const value = (event as unknown as { detail?: { value?: unknown } }).detail?.value;
    if (typeof value === "boolean") form.unlimitedCookingMinutes = value;
}
function changeExcluded(event: { detail: { value: string[] } }): void { form.excludedIngredientIds = event.detail.value.map((value) => Number(value)); }
async function load(): Promise<void> { try { apply(await getPlanningPreferences()); } catch (error: unknown) { feedback.value = readable(error); } }
async function save(): Promise<void> {
    if (saving.value) return;
    saving.value = true;
    feedback.value = "";
    try { apply(await replacePlanningPreferences(toPlanningPreferencesRequest(form))); feedback.value = "偏好已保存"; }
    catch (error: unknown) { feedback.value = readable(error); }
    finally { saving.value = false; }
}
onMounted(async () => { try { await ingredientStore.load(); } catch { /* store state */ } await load(); });
</script>

<style scoped lang="scss">
.section { display: flex; flex-direction: column; gap: $space-md; margin-bottom: $space-lg; }
.section-title { font-size: $text-lg; font-weight: 650; }
.subheading { font-weight: 600; margin-top: $space-sm; }
.field { width: 100%; min-height: 82rpx; padding: 0 $space-md; border: 2rpx solid $color-border; border-radius: $radius-sm; background: #fff; font-size: $text-md; }
.switch-row { display: flex; align-items: center; justify-content: space-between; min-height: 78rpx; }
.check-row { display: flex; align-items: center; gap: $space-sm; min-height: 78rpx; overflow-wrap: anywhere; }
.primary-button { min-height: 80rpx; border-radius: $radius-sm; background: $color-primary; color: #fff; font-size: $text-md; }
.row-detail, .feedback { color: $color-text-secondary; line-height: 1.55; overflow-wrap: anywhere; }
.muted-card { opacity: .8; }
</style>
