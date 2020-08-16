import React, {useState, useEffect} from 'react';
import NavigatonBar from './NavigationBar';
import DetailPage from './DetailPage';
import GuardianDetailPage from './GurdianDetailPage';
import SearchPage from './SearchPage';
import HomePage from './HomePage';
import FavoritesPage from './FavoritesPage';
import {BrowserRouter as Router, Switch, Route} from 'react-router-dom';



function App() {

   const [isGuardian,setIsGuardian] = useState(true);
   const flip = ()=>{
      setIsGuardian(isGuardian =>!isGuardian);
   }
    const [toggleShow, setToggleShow] = useState(true);
    const [windowLocation, setWindowLocation] = useState(window.location.pathname);
    const setPath = (path)=>{
        setWindowLocation(path);
    }

   useEffect(() => {

 },[isGuardian]);

    useEffect(() => {
        const path = window.location.pathname;
        if(path==='/' || path==='/world' ||path==='/politics'||path==='/business'||path==='/technology'||path==='/sports'){
            setToggleShow(true);

        }
        else{
            setToggleShow(false) ;

        }
    },[windowLocation]);

  return(
    <Router>
        <div>

        <NavigatonBar isGuardian={isGuardian} show = {toggleShow} switch = {flip} />

        <Switch>
             <Route path = '/' exact render = { props => (<HomePage {...props} setPath={setPath} source ={isGuardian} key={window.location.pathname}/>)}/>
             <Route path = '/world' exact render = { props => (<HomePage {...props} setPath={setPath} source ={isGuardian} key={window.location.pathname}/>)}/>
             <Route path = '/politics'exact render = { props => (<HomePage {...props} setPath={setPath} source ={isGuardian} key={window.location.pathname}/>)}/>
             <Route path = '/business' exact render = { props => (<HomePage {...props} setPath={setPath} source ={isGuardian} key={window.location.pathname}/>)}/>
             <Route path = '/technology' exact render = { props => (<HomePage {...props} setPath={setPath} source ={isGuardian} key={window.location.pathname}/>)}/>
             <Route path = '/sports' exact render = { props => (<HomePage {...props} setPath={setPath} source ={isGuardian} key={window.location.pathname}/>)}/>
             <Route path = '/detail/:id'  render = { props => (<DetailPage {...props} setPath={setPath}/>)}/>
             <Route path = '/guardian/:id'   render = { props => (<GuardianDetailPage {...props} setPath={setPath}/>)}/>
             <Route path = '/search/:query'  render = { props => (<SearchPage {...props} setPath={setPath} key={window.location.pathname}/>)}/>
             <Route path = '/favorites'  exact render = { props => (<FavoritesPage {...props} setPath={setPath}/>)}/>
        </Switch>
       
        </div>

    </Router>
    
  );
}


export default App;



