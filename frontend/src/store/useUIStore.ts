import { create } from "zustand";

interface ModalState {
  isOpen: boolean;
  type: string | null;
  data?: unknown;
}

interface UIState {
  // Sidebar
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;

  // Modal
  modal: ModalState;
  openModal: (type: string, data?: unknown) => void;
  closeModal: () => void;

  // Loading
  isGlobalLoading: boolean;
  setGlobalLoading: (loading: boolean) => void;
}

export const useUIStore = create<UIState>((set) => ({
  // Sidebar
  sidebarOpen: false,
  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  setSidebarOpen: (open) => set({ sidebarOpen: open }),

  // Modal
  modal: {
    isOpen: false,
    type: null,
    data: undefined,
  },
  openModal: (type, data) =>
    set({
      modal: {
        isOpen: true,
        type,
        data,
      },
    }),
  closeModal: () =>
    set({
      modal: {
        isOpen: false,
        type: null,
        data: undefined,
      },
    }),

  // Loading
  isGlobalLoading: false,
  setGlobalLoading: (loading) => set({ isGlobalLoading: loading }),
}));
