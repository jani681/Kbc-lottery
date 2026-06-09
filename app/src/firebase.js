import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

// Gull Tailors Firebase Configuration
const firebaseConfig = {
  apiKey: "AIzaSyCQXINOqOztzm_f69izGEUDVZf5VsrnY8g",
  authDomain: "gul-tailors-waseem.firebaseapp.com",
  projectId: "gul-tailors-waseem",
  storageBucket: "gul-tailors-waseem.firebasestorage.app",
  messagingSenderId: "620104067440",
  appId: "1:620104067440:web:dfc6bd199d25e6e926c7b5",
  measurementId: "G-XQCSCMJ28D"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Firestore and export it
export const db = getFirestore(app);
