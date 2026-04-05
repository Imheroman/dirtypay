interface SessionLayoutProps {
  children: React.ReactNode;
  params: Promise<{ sessionId: string }>;
}

export default async function SessionLayout({
  children,
}: SessionLayoutProps) {
  return <div className="flex-1 min-w-0">{children}</div>;
}
