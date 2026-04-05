"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { createStoreSchema, type CreateStoreFormData } from "../schemas";

export function useStoreForm() {
  const form = useForm<CreateStoreFormData>({
    resolver: zodResolver(createStoreSchema),
    mode: "onBlur",
    reValidateMode: "onChange",
    defaultValues: {
      name: "",
      address: "",
      description: "",
    },
  });

  return form;
}
