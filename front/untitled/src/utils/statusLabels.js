export const CONTACT_STATUS_OPTIONS = [
  { value: 'PENDING', label: '待处理' },
  { value: 'ACCEPTED', label: '已接单' },
  { value: 'REJECTED', label: '已拒绝' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CANCELLED', label: '已取消' },
]

const CONTACT_STATUS_LABEL_MAP = CONTACT_STATUS_OPTIONS.reduce((map, option) => {
  map[option.value] = option.label
  return map
}, {})

export function getContactStatusLabel(status) {
  if (!status) return '未知'
  return CONTACT_STATUS_LABEL_MAP[status] || status
}

export const CERTIFICATION_STATUS_OPTIONS = [
  { value: 'PENDING', label: '待审核' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已驳回' },
]

const CERTIFICATION_STATUS_LABEL_MAP = CERTIFICATION_STATUS_OPTIONS.reduce((map, option) => {
  map[option.value] = option.label
  return map
}, {})

export function getCertificationStatusLabel(status) {
  if (!status) return '未知'
  return CERTIFICATION_STATUS_LABEL_MAP[status] || status
}
