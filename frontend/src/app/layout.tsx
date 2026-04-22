import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Interview Intel — Real interview experiences, not guesses',
  description: 'Aggregated interview experiences, compensation data, and prep questions for any company and role.',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
