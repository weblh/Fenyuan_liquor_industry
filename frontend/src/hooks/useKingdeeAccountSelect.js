import { useCallback, useEffect, useState } from 'react'
import { kingdeeAccountSetApi } from '@/api/modules/kingdee'

export function useKingdeeAccountSelect() {
  const [accounts, setAccounts] = useState([])
  const [loading, setLoading] = useState(true)
  const [selectedAccountId, setSelectedAccountId] = useState()

  const loadAccounts = useCallback(async () => {
    setLoading(true)
    try {
      const [enabled, defaultAccount] = await Promise.all([
        kingdeeAccountSetApi.listEnabled(),
        kingdeeAccountSetApi.getDefault().catch(() => null),
      ])
      const list = Array.isArray(enabled) ? enabled : []
      setAccounts(list)
      if (defaultAccount && list.some((item) => item.id === defaultAccount.id)) {
        setSelectedAccountId(defaultAccount.id)
      } else if (list.length > 0) {
        setSelectedAccountId(list[0].id)
      } else {
        setSelectedAccountId(undefined)
      }
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadAccounts()
  }, [loadAccounts])

  const selectedAccount = accounts.find((item) => item.id === selectedAccountId)

  return {
    accounts,
    loading,
    selectedAccountId,
    setSelectedAccountId,
    selectedAccount,
    reloadAccounts: loadAccounts,
  }
}
