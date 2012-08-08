namespace(lib.FormTagLib).with {
    entry(title:'Name', field:'name') {
        textbox()
    }
    entry(title:'URL', field:'url') {
        textbox()
    }
    entry {
        div(align:'right') {
            repeatableDeleteButton()
        }
    }
}