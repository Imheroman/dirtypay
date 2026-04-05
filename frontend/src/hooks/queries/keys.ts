// Query Key Factory
// 일관된 쿼리 키 관리를 위한 중앙 정의

export const queryKeys = {
  // Auth
  auth: {
    all: ["auth"] as const,
    user: () => [...queryKeys.auth.all, "user"] as const,
  },

  // User (Member)
  users: {
    all: ["users"] as const,
    detail: (id: number) => [...queryKeys.users.all, "detail", id] as const,
  },

  // Session
  sessions: {
    all: ["sessions"] as const,
    lists: () => [...queryKeys.sessions.all, "list"] as const,
    list: (filters?: Record<string, unknown>) =>
      [...queryKeys.sessions.lists(), filters] as const,
    archived: () => [...queryKeys.sessions.all, "archived"] as const,
    details: () => [...queryKeys.sessions.all, "detail"] as const,
    detail: (id: string) => [...queryKeys.sessions.details(), id] as const,
    invite: (code: string) => [...queryKeys.sessions.all, "invite", code] as const,
    joinRequests: (sessionId: string, status?: string) =>
      status
        ? [...queryKeys.sessions.all, "joinRequests", sessionId, status] as const
        : [...queryKeys.sessions.all, "joinRequests", sessionId] as const,
  },

  // Organization (Node, Member)
  organization: {
    all: ["organization"] as const,
    nodes: (sessionId: string) =>
      [...queryKeys.organization.all, "nodes", sessionId] as const,
    members: (sessionId: string) =>
      [...queryKeys.organization.all, "members", sessionId] as const,
  },

  // Round
  rounds: {
    all: ["rounds"] as const,
    lists: (sessionId: string) =>
      [...queryKeys.rounds.all, "list", sessionId] as const,
    details: () => [...queryKeys.rounds.all, "detail"] as const,
    detail: (id: string) => [...queryKeys.rounds.details(), id] as const,
    participants: (roundId: string) =>
      [...queryKeys.rounds.all, "participants", roundId] as const,
  },

  // Order & Menu
  orders: {
    all: ["orders"] as const,
    byRound: (roundId: string) =>
      [...queryKeys.orders.all, "round", roundId] as const,
  },
  // Round Groups
  roundGroups: {
    all: ["roundGroups"] as const,
    byRound: (roundId: string) =>
      [...queryKeys.roundGroups.all, "round", roundId] as const,
  },

  // Seller
  sellers: {
    all: ["sellers"] as const,
    detail: (id: number) => [...queryKeys.sellers.all, "detail", id] as const,
    me: () => [...queryKeys.sellers.all, "me"] as const,
    storeList: (sellerId: number) =>
      [...queryKeys.sellers.all, "stores", sellerId] as const,
  },

  // Store Menu
  storeMenus: {
    all: ["storeMenus"] as const,
    byStore: (storeId: number) =>
      [...queryKeys.storeMenus.all, "store", storeId] as const,
    detail: (id: number) =>
      [...queryKeys.storeMenus.all, "detail", id] as const,
  },

  // Store Order
  storeOrders: {
    all: ["storeOrders"] as const,
    byStore: (storeId: number) =>
      [...queryKeys.storeOrders.all, "store", storeId] as const,
  },

  // Wallet
  wallet: {
    all: ["wallet"] as const,
    me: () => [...queryKeys.wallet.all, "me"] as const,
    transactions: (params?: Record<string, unknown>) =>
      [...queryKeys.wallet.all, "transactions", params] as const,
  },

  // Settlement
  settlement: {
    all: ["settlement"] as const,
    session: (sessionId: string) =>
      [...queryKeys.settlement.all, "session", sessionId] as const,
    round: (roundId: string) =>
      [...queryKeys.settlement.all, "round", roundId] as const,
    member: (sessionId: string, memberId: string) =>
      [...queryKeys.settlement.all, "member", sessionId, memberId] as const,
    group: (roundId: string, groupId: string) =>
      [...queryKeys.settlement.all, "group", roundId, groupId] as const,
    groupAmounts: (roundId: string, groupId: string) =>
      [...queryKeys.settlement.all, "groupAmounts", roundId, groupId] as const,
    transfers: (sessionId: string) =>
      [...queryKeys.settlement.all, "transfers", sessionId] as const,
  },

  // Store & Menu
  stores: {
    all: ["stores"] as const,
    lists: () => [...queryKeys.stores.all, "list"] as const,
    list: (filters?: Record<string, unknown>) =>
      [...queryKeys.stores.lists(), filters] as const,
    details: () => [...queryKeys.stores.all, "detail"] as const,
    detail: (storeId: string) => [...queryKeys.stores.details(), storeId] as const,
    statistics: (storeId: string) =>
      [...queryKeys.stores.all, "statistics", storeId] as const,
    popularMenus: (storeId: string) =>
      [...queryKeys.stores.all, "popularMenus", storeId] as const,
    menus: {
      all: (storeId: string) => [...queryKeys.stores.all, "menus", storeId] as const,
      available: (storeId: string) =>
        [...queryKeys.stores.all, "menus", storeId, "available"] as const,
      detail: (storeId: string, menuId: string) =>
        [...queryKeys.stores.all, "menus", storeId, "detail", menuId] as const,
    },
    orders: {
      all: (storeId: string) => [...queryKeys.stores.all, "orders", storeId] as const,
      list: (storeId: string, filters?: Record<string, unknown>) =>
        [...queryKeys.stores.all, "orders", storeId, "list", filters] as const,
      detail: (storeId: string, orderId: string) =>
        [...queryKeys.stores.all, "orders", storeId, "detail", orderId] as const,
    },
    reviews: {
      all: (storeId: string) => [...queryKeys.stores.all, "reviews", storeId] as const,
      detail: (storeId: string, reviewId: string) =>
        [...queryKeys.stores.all, "reviews", storeId, "detail", reviewId] as const,
      rating: (storeId: string) =>
        [...queryKeys.stores.all, "reviews", storeId, "rating"] as const,
    },
  },
};
