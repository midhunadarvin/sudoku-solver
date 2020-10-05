// Random generator 
export function randomGenerator(num) {
    return Math.floor((Math.random() * num + 1));
}

export function delay(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}