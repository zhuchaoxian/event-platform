export function formatTime(ts) {
  if (!ts) return ''
  var d = new Date(ts)
  return d.toLocaleString()
}

export function formatJson(str) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch (e) {
    return str
  }
}

export var EVENT_TYPES = ['parking', 'pedestrian', 'congestion', 'spilled_items', 'wrong_way']

export var EVENT_TYPE_LABELS = {
  parking: '违停',
  pedestrian: '行人',
  congestion: '拥堵',
  spilled_items: '遗撒物',
  wrong_way: '逆行'
}
