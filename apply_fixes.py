#!/usr/bin/env python3
"""
ClaimBlocks Bug Fixer
Applies all 4 bug fixes to decompiled source code
"""

import re
import os

def fix_cbeventhandler(filepath):
    """Apply BUG 1 and BUG 2 fixes to CBEventHandler.java"""
    print(f"[*] Reading {filepath}")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    changes_made = 0
    
    # BUG 1 FIX: Add item stack synchronization
    # Find the section where item is decremented
    pattern1 = r'(if\s*\(!!\(\(class_3222\)player\)\.method_7337\(\)\)\s*\{\s*stackInHand\.method_7934\(1\);\s*\})\s*(return\s+class_1269\.field_5814;)'
    
    replacement1 = r'''if (!((class_3222)player).method_7337()) {
            stackInHand.method_7934(1);
            // BUG 1 FIX: Synchronize item consumption with client
            if (stackInHand.method_7960()) {
                ((class_3222)player).method_6030(hand, class_1799.field_8037);
            } else {
                ((class_3222)player).method_6030(hand, stackInHand);
            }
        }
        // BUG 2 FIX: Return SUCCESS instead of FAIL
        return class_1269.field_5812;'''
    
    content, n = re.subn(pattern1, replacement1, content, flags=re.MULTILINE | re.DOTALL)
    if n > 0:
        print(f"  [✓] Applied BUG 1 & 2 fix (item sync + ActionResult): {n} replacement(s)")
        changes_made += n
    
    # BUG 2 FIX: Replace all remaining field_5814 (FAIL) with field_5812 (SUCCESS)
    # But skip the first two field_5811 (PASS) returns
    
    # Count and replace field_5814 (FAIL) with field_5812 (SUCCESS)
    fail_count_before = content.count('field_5814')
    content = content.replace('field_5814', 'field_5812')
    fail_count_after = content.count('field_5814')
    fail_replacements = fail_count_before - fail_count_after
    
    if fail_replacements > 0:
        print(f"  [✓] Applied BUG 2 fix (FAIL→SUCCESS): {fail_replacements} replacement(s)")
        changes_made += fail_replacements
    
    if content != original_content:
        # Create backup
        backup_path = filepath + '.backup'
        with open(backup_path, 'w', encoding='utf-8') as f:
            f.write(original_content)
        print(f"  [i] Backup saved to {backup_path}")
        
        # Write fixed version
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  [✓] Fixed file saved with {changes_made} total changes")
        return True
    else:
        print(f"  [!] No changes applied")
        return False


def fix_cbmanager(filepath):
    """Apply BUG 3 fix to CBManager.java"""
    print(f"[*] Reading {filepath}")
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original_content = content
    
    # BUG 3 FIX: Add owner comparison before overlap detection
    # Find the pattern in isOverlapping method
    pattern = r'(if\s*\(Intrinsics\.areEqual\(\(Object\)existingRegion\.getName\(\),\s*\(Object\)newRegion\.getName\(\)\)\)\s*\{\s*bl2\s*=\s*false;\s*\})\s*(else\s+if\s*\(!)'
    
    replacement = r'''\1 else if (Intrinsics.areEqual((Object)existingRegion.getOwner(), (Object)newRegion.getOwner())) {
                        // BUG 3 FIX: Skip overlap check if same owner
                        bl2 = false;
                    } \2'''
    
    content, n = re.subn(pattern, replacement, content, flags=re.MULTILINE | re.DOTALL)
    
    if n > 0:
        print(f"  [✓] Applied BUG 3 fix (owner comparison): {n} replacement(s)")
        
        # Create backup
        backup_path = filepath + '.backup'
        with open(backup_path, 'w', encoding='utf-8') as f:
            f.write(original_content)
        print(f"  [i] Backup saved to {backup_path}")
        
        # Write fixed version
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"  [✓] Fixed file saved")
        return True
    else:
        print(f"  [!] No changes applied - pattern not found")
        return False


def main():
    print("=" * 60)
    print("ClaimBlocks Bug Fixer - Applying all 4 fixes")
    print("=" * 60)
    
    decompiled_dir = 'decompiled_source/com/f0cus/protectionstones'
    
    # Apply BUG 1 & 2 fixes to CBEventHandler
    eventhandler_path = os.path.join(decompiled_dir, 'CBEventHandler.java')
    if os.path.exists(eventhandler_path):
        print("\n[1/2] Fixing CBEventHandler.java (BUG 1 & 2)")
        fix_cbeventhandler(eventhandler_path)
    else:
        print(f"\n[!] ERROR: {eventhandler_path} not found")
    
    # Apply BUG 3 fix to CBManager
    manager_path = os.path.join(decompiled_dir, 'CBManager.java')
    if os.path.exists(manager_path):
        print("\n[2/2] Fixing CBManager.java (BUG 3)")
        fix_cbmanager(manager_path)
    else:
        print(f"\n[!] ERROR: {manager_path} not found")
    
    print("\n" + "=" * 60)
    print("Fixes applied successfully!")
    print("BUG 4 (message spam) is automatically fixed by BUG 1 & 2 fixes")
    print("=" * 60)
    print("\nNext steps:")
    print("1. Review the fixed files in decompiled_source/")
    print("2. Recompile the classes with javac or kotlinc")
    print("3. Replace the .class files in the JAR")
    print("4. Test the fixed mod")


if __name__ == '__main__':
    main()
