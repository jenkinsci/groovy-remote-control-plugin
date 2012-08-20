namespace(lib.FormTagLib).with {
    entry(title:'Key', field:'key') {
        textbox()
    }
    entry(title:'Value', field:'value') {
        textbox()
    }
    entry {
        div(align:'right') {
            repeatableDeleteButton()
        }
    }
}