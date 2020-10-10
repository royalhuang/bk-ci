interface User {
    [index: string]: any
    id?: string
    isAuthenticated?: boolean
    username?: string
    avatarUrl?: string
    chineseName?: string
    phone?: string
    email?: string
}

interface ObjectMap {
    [index: string]: any
}

interface Route {
    path: string,
    name: string,
    component: any,
    children: any[],
    meta: any
}
interface Window {
    Pages: any
    eventBus: object
    devops: object
    iframeUtil: ObjectMap
    allServices: ObjectMap[]
    projectList: ObjectMap[]
    serviceObject: ObjectMap
    currentPage: subService
    userInfo: User
    vuexStore: any
    setLsCacheItem: Function
    getLsCacheItem: Function
    setProjectIdCookie: Function
    JSONP: Function
    GLOBAL_PID: string
    getLoginUrl: Function
    attachEvent(event: string, listener: EventListener): boolean
    detachEvent(event: string, listener: EventListener): void
}
interface subService {
    'collected': boolean
    'css_url': string
    'id': string
    'iframe_url': string
    'inject_type': string
    'show_project_list': boolean
    'show_nav': boolean
    'js_url': string
    'link': string
    'name': string
    'status': string
    'link_new': string,
    'project_id_type': string
}

interface Permission {
    resource: string
    option: string
}

declare module '*.vue' {
    import Vue from 'vue'
    export default Vue
}

declare const LOGIN_SERVICE_URL: string
declare const GW_URL_PREFIX: string
declare const DOCS_URL_PREFIX: string
declare const DEVOPS_LS_VERSION: string
declare const ENTERPRISE_VERSION: string
declare module '*.png'

declare const X_DEVOPS_PROJECT_ID: string

