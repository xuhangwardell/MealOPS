/// <reference types="vite/client" />
/// <reference types="@dcloudio/types" />

interface ImportMetaEnv {
    readonly VITE_API_BASE_URL?: string;
    readonly VITE_API_PROXY_TARGET?: string;
    readonly VITE_API_TIMEOUT_MS?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}

declare module "*.vue" {
    import type { DefineComponent } from "vue";
    const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
    export default component;
}

declare module "vue" {
    type Hooks = App.AppInstance & Page.PageInstance;
    interface ComponentCustomOptions extends Hooks {}
}

export {};
