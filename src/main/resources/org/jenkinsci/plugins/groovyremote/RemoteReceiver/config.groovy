namespace(lib.FormTagLib).with {
    entry(field:'name') {
        textbox()
    }
    entry(field:'url') {
        textbox()
    }
    entry {
        div(align:'right') {
            repeatableDeleteButton()
        }
    }
}