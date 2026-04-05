import Link from 'next/link';

const footerLinks = [
  { label: '공지사항', href: '#' },
  { label: '이용약관', href: '#' },
  { label: '개인정보처리방침', href: '#' },
];

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-background">
      <div className="container mx-auto px-4 md:px-6 py-6">
        {/* 링크 */}
        <nav className="flex flex-wrap justify-center gap-4 md:gap-6 mb-4">
          {footerLinks.map((link) => (
            <Link
              key={link.label}
              href={link.href}
              className="text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              {link.label}
            </Link>
          ))}
        </nav>

        {/* 저작권 */}
        <p className="text-center text-sm text-muted-foreground">
          © {currentYear} Dirty Pay. All rights reserved.
        </p>
      </div>
    </footer>
  );
}
