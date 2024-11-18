import OncoTree, { ToolbarOptions, buildToolbar } from "@oncokb/oncotree";
import { useEffect, useRef } from "react";

const TOOLBAR_CONTAINER_ID = 'oncotree-toolbar-container';

export interface IToolbarProps {
    oncoTree: OncoTree, 
    options?: ToolbarOptions
}

export default function Toolbar({oncoTree, options}: IToolbarProps) {
    const toolbarContainerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (toolbarContainerRef.current) {
            toolbarContainerRef.current.innerHTML = '';
            buildToolbar(TOOLBAR_CONTAINER_ID, oncoTree, options);
        }
    }, [oncoTree, options]);

    return (
        <div ref={toolbarContainerRef} id={TOOLBAR_CONTAINER_ID}/>
    )
}