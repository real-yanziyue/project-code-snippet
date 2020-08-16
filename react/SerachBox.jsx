import React, { Component } from 'react';
import AsyncSelect from "react-select/lib/Async";
import {withRouter} from 'react-router-dom';


class SelectKeyWord extends Component {
    constructor(props) {
        super(props);
        this.state = {
            inputValue : ''
        }
        this.timer = null;
        this.toSearchResult = this.toSearchResult.bind(this);
        this.handleInputChange = this.handleInputChange.bind(this);
        this.loadOptions = this.loadOptions.bind(this);
    }

   

    loadOptions (inputValue){
   
        return new Promise(resolve => {

            clearTimeout(this.timer);
            this.timer = setTimeout(async ()=>{
                    const response = await fetch('https://api.cognitive.microsoft.com/bing/v7.0/suggestions?q=' + inputValue,{
                        method: "GET",
                        headers: {
                            'Ocp-Apim-Subscription-Key': '......................'
                        },
                    })
                    const result = await response.json()

                    console.log("result", result)
                    const tempArray = [];
                    result.suggestionGroups[0].searchSuggestions.map((element) => {
                        tempArray.push({ label:`${element.displayText}`,value: `${element.displayText}` });
                    });
                    console.log(tempArray);
                    resolve(tempArray)
                },1000
            )
        }

        )

    }



    toSearchResult (input) {
        this.setState({inputValue: input.value});
        this.props.history.push('/search/' + input.value);
        console.log('to search result triggerd')
   
    }

    handleInputChange(newValue){
        const inputValue = newValue.replace(/\W/g, '');
        this.setState({inputValue : inputValue});
        console.log('inputChangetriggered')
        return inputValue;
    }

    render() {
        // alert('render search box')

        return (
            <div className='searchBox' style={{width:"300px", minWidth:'200px'}} >
                <AsyncSelect  noOptionsMessage ={()=>'no match'} onInputChange ={this.handleInputChange} loadOptions = {this.loadOptions} onChange = {this.toSearchResult} placeholder = 'Enter Keyword...'/>
            </div>
        );
    }
}

export default withRouter(SelectKeyWord);










