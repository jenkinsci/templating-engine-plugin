(function() {

function onLoad(){
    process()

    // Callback function to execute when mutations are observed
    var callback = function(mutationsList, observer) {
        for(var mutation of mutationsList) {
            if (mutation.type == 'childList') {
                process()
            }
        }
    };

    // Create an observer instance linked to the callback function
    var observer = new MutationObserver(callback);

    // Options for the observer (which mutations to observe)
    var config = { childList: true, subtree: true };

    // Start observing the target node for configured mutations
    observer.observe(document, config);
}

function process(){
    document.querySelectorAll("span[first-line='true']").forEach( (element) => {
        if(element.hasAttribute("jte-processed")){
            return
        }
        element.setAttribute("jte-processed", true)
        element.textContent = element.textContent.replace(/(\r\n|\n|\r)/gm,"")
        element.innerHTML += " <span class='jte-collapsible' isHidden='false' onclick='jteCollapse(this)'>(hide)</span><br>"
        if(element.hasAttribute("initially-hidden")){
            jteHide(element.querySelector("span"))
        }
    });
}

// Run on page load
if (document.readyState === 'complete') {
    onLoad();
} else {
    Behaviour.addLoadEvent(onLoad);
}

}());

function jteCollapse(link){
    if(link.getAttribute("isHidden") === 'false'){
        jteHide(link)
    }else{
        jteShow(link)
    }
}

function jteHide(link){
    link.setAttribute("isHidden",'true');
    link.textContent = "(show)";
    link.style.visibility = "visible";
    var id = link.parentNode.getAttribute("jte-id");
    document.querySelectorAll(`span[jte-id='${id}'][first-line='false']`).forEach( (element) => {
        element.style.display = 'none';

        previous = element.previousSibling;
        if(previous != null && previous.getAttribute("class").includes("timestamp")) {
            previous.style.display = 'none';
        }
    });
}
function jteShow(link){
    link.setAttribute("isHidden",'false');
    link.textContent = "(hide)";
    link.style.visibility = "";
    var id = link.parentNode.getAttribute("jte-id");
    document.querySelectorAll(`span[jte-id='${id}'][first-line='false']`).forEach( (element) => {
        element.style.display = 'inline';

        previous = element.previousSibling;
        if(previous != null && previous.getAttribute("class").includes("timestamp")) {
            previous.style.display = 'inline';
        }
    });
}
