export interface MenuConfigItem {
  key: string;
  title: string;
  path: string;
  permission?: string;
}

export const MENU_ITEMS: MenuConfigItem[] = [
  { key: "dashboard", title: "工作台", path: "/dashboard" },
  { key: "product", title: "商品管理", path: "/product", permission: "pms:product:read" },
  { key: "order", title: "订单管理", path: "/order", permission: "oms:order:read" },
  { key: "rbac", title: "权限管理", path: "/rbac", permission: "admin:read" },
  { key: "loginLog", title: "登录日志", path: "/login-log", permission: "admin:read" }
];
