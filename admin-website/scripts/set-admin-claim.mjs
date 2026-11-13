import { applicationDefault, initializeApp } from 'firebase-admin/app'
import { getAuth } from 'firebase-admin/auth'
import { getFirestore, FieldValue } from 'firebase-admin/firestore'

const mobile = (process.argv[2] || '').replace(/\D/g, '').replace(/^91(?=\d{10}$)/, '')
if (!/^[6-9]\d{9}$/.test(mobile)) {
  console.error('Usage: npm run set-admin -- 9876543210')
  process.exit(1)
}
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error('Set GOOGLE_APPLICATION_CREDENTIALS to the path of your private Firebase service-account JSON first.')
  process.exit(1)
}
initializeApp({ credential: applicationDefault(), projectId: 'cityseva-dhule' })
const auth = getAuth()
const db = getFirestore()
const emailAlias = `91${mobile}@cityseva.app`
const account = await auth.getUserByEmail(emailAlias)
await auth.setCustomUserClaims(account.uid, { ...(account.customClaims || {}), admin: true })
await db.collection('users').doc(account.uid).set({
  name: 'Admin',
  phoneNumber: mobile,
  role: 'admin',
  active: true,
  status: 'active',
  createdAt: FieldValue.serverTimestamp()
}, { merge: true })
console.log(`Admin claim assigned to +91${mobile}. UID: ${account.uid}`)
console.log('Sign out and sign in again in the website so the refreshed token contains the claim.')
