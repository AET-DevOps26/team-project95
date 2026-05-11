import { createBrowserRouter, RouterProvider } from 'react-router-dom'
// import './App.css'
import HomePage from './pages/HomePage'
import DetailedThesisPage from './pages/DetailedThesisPage'

function App() {

    const router = createBrowserRouter([
        {
            path: "/",
            element: <HomePage/>
        },
        {
            path: "thesis",
            element: <DetailedThesisPage/>
        },
    ])

    return (
        <RouterProvider router={router}/>
    )
}

export default App
