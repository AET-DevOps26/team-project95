import { createBrowserRouter, RouterProvider } from 'react-router-dom'
import './App.css'
import HomePage from './pages/HomePage'
import DetailedThesisPage from './pages/DetailedThesisPage'
import { SearchStateProvider } from './state/searchState'

function App() {

    const router = createBrowserRouter([
        {
            path: "/",
            element: <HomePage/>
        },
        {
            path: "thesis/:thesisId?",
            element: <DetailedThesisPage/>
        },
    ])

    return (
        <SearchStateProvider>
            <RouterProvider router={router}/>
        </SearchStateProvider>
    )
}

export default App
