import { applicationDefault, initializeApp } from 'firebase-admin/app'
import { getAuth } from 'firebase-admin/auth'

const phoneNumber = process.argv[2]
if (!phoneNumber) {
  console.error('Usage: npm run set-admin -- +919876543210')
  process.exit(1)
}
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to the path of your private Firebase service-account JSON first.')
  process.exit(1)
}
initializeApp({ credential: applicationDefault(), projectId: 'cityseva-dhule' })
const auth = getAuth()
const account = await auth.getUserByPhoneNumber(phoneNumber)
await auth.setCustomUserClaims(account.uid, { ...(account.customClaims || {}), admin: true })
console.log(`Admin claim assigned to ${phoneNumber}. UID: ${account.uid}`)
console.log('Sign out and sign in again in the website so the refreshed token contains the claim.')
