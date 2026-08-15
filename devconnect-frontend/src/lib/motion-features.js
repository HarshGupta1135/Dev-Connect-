/**
 * Motion's animation engine, isolated so it can become its own chunk.
 *
 * App.jsx must NOT dynamically import 'motion/react' directly: it also imports
 * LazyMotion and m from it statically, and a module that is both statically and
 * dynamically imported gets welded into the static chunk whole — which is how the
 * main bundle once grew by 65 kB gzipped. Routing the dynamic import through this
 * file gives Rollup a separate module to split on: the barrel's tiny orchestrator
 * exports stay in main, the engine lands here, loaded after first paint.
 */
export { domAnimation as default } from 'motion/react';
