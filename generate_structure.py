import os
import json

# Define the target directory relative to the current working directory
target_dir = os.path.join('app', 'src', 'main')
output_file = 'project_structure.json'

def get_directory_structure(root_path):
    """
    Recursively builds a nested dictionary representing the file structure.
    """
    structure = {}
    try:
        # List all items in the directory
        items = os.listdir(root_path)
        # Sort items to ensure deterministic output
        items.sort()
        
        for item in items:
            item_path = os.path.join(root_path, item)
            
            if os.path.isdir(item_path):
                # If directory, recurse
                structure[item] = get_directory_structure(item_path)
            else:
                # If file, just mark it (or you could store file size, etc.)
                structure[item] = "file"
                
    except OSError as e:
        print(f"Error accessing {root_path}: {e}")
        return None
        
    return structure

if __name__ == "__main__":
    if os.path.exists(target_dir):
        print(f"Scanning directory: {target_dir}")
        result = get_directory_structure(target_dir)
        
        if result is not None:
            try:
                with open(output_file, 'w') as f:
                    json.dump(result, f, indent=4)
                print(f"Success! JSON structure written to {output_file}")
            except Exception as e:
                print(f"Failed to write JSON file: {e}")
        else:
            print("Failed to generate structure.")
    else:
        print(f"Target directory '{target_dir}' does not found in {os.getcwd()}")
