// 菜单 menuKey -> Element Plus 图标组件名（全局已注册）
export const menuIconMap = {
  dashboard: 'Odometer',
  rooms: 'ChatDotRound',
  'my-messages': 'ChatLineSquare',
  broadcast: 'Promotion',
  audit: 'DocumentChecked',
  metrics: 'DataLine',
  configs: 'SetUp',
  system: 'Management',
  'system:users': 'User',
  'system:roles': 'UserFilled',
};

export function iconOf(menuKey) {
  return menuIconMap[menuKey] || 'Menu';
}
