const fs = require('fs');
const path = require('path');

// Target the actual android project source files situated outside the executive applet directory
const srcDir = '../src/main/java/com/example/';

function replaceInFile(filePath) {
    if (!fs.existsSync(filePath)) return;
    let content = fs.readFileSync(filePath, 'utf8');
    let hasChanges = false;

    // Replace package statements
    const newContent = content
        .replace(/package\s+com\.example\b/g, () => {
            hasChanges = true;
            return 'package com.scholarvault';
        })
        .replace(/import\s+com\.example\./g, () => {
            hasChanges = true;
            return 'import com.scholarvault.';
        });

    if (hasChanges) {
        fs.writeFileSync(filePath, newContent, 'utf8');
        console.log(`Updated: ${filePath}`);
    }
}

function traverse(dir) {
    if (!fs.existsSync(dir)) return;
    const files = fs.readdirSync(dir);
    for (const file of files) {
        const fullPath = path.join(dir, file);
        const stat = fs.statSync(fullPath);
        if (stat.isDirectory()) {
            traverse(fullPath);
        } else if (stat.isFile() && file.endsWith('.kt')) {
            replaceInFile(fullPath);
        }
    }
}

console.log('Starting refactoring of com.example packages to com.scholarvault...');
traverse(srcDir);
console.log('Finished refactoring successfully!');
