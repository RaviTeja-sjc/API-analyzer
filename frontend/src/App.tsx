
import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './routes/AppRoutes';
import './index.css';

function App() {
  return (
    <BrowserRouter>
      <div className="app-container">
        <AppRoutes />
      </div>
    </BrowserRouter>
  );
}

export default App;
