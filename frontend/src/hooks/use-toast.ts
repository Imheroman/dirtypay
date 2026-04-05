import { toast } from 'sonner';

export function useToast() {
  return {
    toast,
    success: (message: string, description?: string) =>
      toast.success(message, { description, duration: 3000 }),
    error: (message: string, description?: string) =>
      toast.error(message, { description, duration: 5000 }),
    info: (message: string, description?: string) =>
      toast.info(message, { description, duration: 2000 }),
    dismiss: toast.dismiss,
  };
}

export { toast } from 'sonner';
